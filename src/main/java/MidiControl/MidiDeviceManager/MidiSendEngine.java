package MidiControl.MidiDeviceManager;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Logger;

import MidiControl.Telemetry.MidiTelemetry;
import MidiControl.Telemetry.TelemetryListener;
import MidiControl.Telemetry.TelemetryPublisher;

public final class MidiSendEngine implements MidiIngressListener {
    private static final Logger logger = Logger.getLogger(MidiSendEngine.class.getName());
    private static boolean debug = false;

    public enum ThroughputProfile {
        SAFE_DIN (3125, 128, 1_500_000L, 4L, 98),
        FAST_USB (40000, 512,   200_000L, 2L, 512),
        FAST_RTP (80000, 1024,    50_000L, 2L, 1024);

        final int bytesPerSecond;
        final int sysexChunkBytes;
        final long interChunkNanos;
        final long pollDelay;
        final int burstBytes;

        ThroughputProfile(int bps, int chunk, long gapNs, long pollDelay, int burstcap) {
            this.bytesPerSecond = bps;
            this.sysexChunkBytes = chunk;
            this.interChunkNanos = gapNs;
            this.pollDelay = pollDelay;
            this.burstBytes = burstcap;
        }
    }

    private static final Logger log = Logger.getLogger(MidiSendEngine.class.getName());

    private final MidiOutput midiOut;

    private final ArrayBlockingQueue<byte[]> normalLane;

    private final ArrayBlockingQueue<byte[]> realtimeLane;

    private final Object sysexLock = new Object();
    private final LinkedHashMap<Long, byte[]> pendingSysex;
    private final int pendingSysexCapacity;

    private final CoalesceEngine coalesceEngine;

    private final ExecutorService worker;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile ThroughputProfile profile = ThroughputProfile.SAFE_DIN;

    private final Object tokenLock = new Object();
    private double tokens;
    private long lastRefillNs;

    private final MidiTelemetry telemetry;
    private TelemetryPublisher telemetryPublisher;
    private TelemetryListener telemetryListener = json -> {};

    private long ingressWindowStartNs;
    private long ingressBytesInWindow;
    private double ingressBpsEwma;

    private static final long INGRESS_WINDOW_NS = 500_000_000L;
    private static final double INGRESS_EWMA_ALPHA = 0.25;

    /**
     * @param out             MIDI output abstraction
     * @param queueCapacity   capacity for normal lane
     * @param sysexCapacity   max number of distinct coalesced SysEx controls pending
     * @param coalesceEngine  keying engine (returns -1 for non-coalescable/unkeyable)
     */
    public MidiSendEngine(MidiOutput out, int queueCapacity, int sysexCapacity) {
        this.midiOut = out;
        this.normalLane = new ArrayBlockingQueue<>(queueCapacity);
        this.realtimeLane = new ArrayBlockingQueue<>(64);
        this.telemetry = new MidiTelemetry(this);

        this.pendingSysexCapacity = Math.max(1, sysexCapacity);
        this.pendingSysex = new LinkedHashMap<>(this.pendingSysexCapacity, 0.75f, false);
        this.coalesceEngine = new CoalesceEngine();

        this.worker = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "midi-send-worker");
            t.setDaemon(true);
            return t;
        });

        long now = System.nanoTime();
        synchronized (tokenLock) {
            this.tokens = 0.0;
            this.lastRefillNs = now;
            this.ingressWindowStartNs = now;
            this.ingressBytesInWindow = 0;
            this.ingressBpsEwma = 0.0;
        }
    }

    public static void enableDebug(){debug = true;}

    public void setThroughputProfile(ThroughputProfile p) {
        this.profile = p;
        log.info("Throughput profile set to " + p);
    }

    public ThroughputProfile getThroughputProfile() {
        return profile;
    }

    public void setTelemetryListener(TelemetryListener listener) {
        if (listener != null) {
            this.telemetryListener = listener;
        }
    }

    /**
     * Offer a message into the engine.
     */
    public boolean offer(byte[] msg) {
        if (msg == null || msg.length == 0) return false;

        if (isRealtime(msg)) {
            boolean ok = realtimeLane.offer(msg);
            if (!ok) telemetry.dropped(msg.length);
            return ok;
        }

        if (isSysex(msg)) {
            if (tryCoalesceSysex(msg)) {
                return true;
            }
        }

        boolean ok = normalLane.offer(msg);
        if (!ok) telemetry.dropped(msg.length);
        return ok;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) return;
        telemetryPublisher = new TelemetryPublisher(telemetry, telemetryListener, 5);
        worker.execute(this::runLoop);
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) return;
        if (telemetryPublisher != null) telemetryPublisher.shutdown();
        worker.shutdownNow();

        realtimeLane.clear();
        normalLane.clear();
        synchronized (sysexLock) {
            pendingSysex.clear();
        }
    }

    private void runLoop() {
        while (running.get()) {
            try {
                byte[] msg = pollNext();
                if (msg == null) continue;

                if (isSysex(msg) && profile != ThroughputProfile.FAST_RTP) {
                    sendSysexChunked(msg, profile.sysexChunkBytes, profile.interChunkNanos);
                } else {
                    sendWithPacing(msg);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warning("Failed to send MIDI: " + e.getMessage());
            }
        }
    }

    /**
     * Poll priority:
     */
    private byte[] pollNext() throws InterruptedException {
        byte[] rt = realtimeLane.poll();
        if (rt != null) return rt;
        byte[] sx = pollCoalescedSysex();
        if (sx != null) return sx;
        byte[] normal = normalLane.poll(250, TimeUnit.MICROSECONDS);
        if (normal != null) return normal;
        return realtimeLane.poll();
    }

    private byte[] pollCoalescedSysex() {
        synchronized (sysexLock) {
            Iterator<Map.Entry<Long, byte[]>> it = pendingSysex.entrySet().iterator();
            if (!it.hasNext()) return null;
            Map.Entry<Long, byte[]> e = it.next();
            byte[] msg = e.getValue();
            it.remove();
            return msg;
        }
    }

    /**
     * Try to insert into the coalesced map.
     * Returns true if accepted into coalesced lane, false if it should go to normal lane.
     */
    private boolean tryCoalesceSysex(byte[] msg) {
        if (!isSysexFramed(msg)) return false;

        long key = coalesceEngine.getKey(msg);
        if (key == -1L) return false;

        synchronized (sysexLock) {
            if (!pendingSysex.containsKey(key) && pendingSysex.size() >= pendingSysexCapacity) {
                Iterator<Map.Entry<Long, byte[]>> it = pendingSysex.entrySet().iterator();
                if (it.hasNext()) {
                    Map.Entry<Long, byte[]> evicted = it.next();
                    byte[] dropped = evicted.getValue();
                    it.remove();
                    if (dropped != null) telemetry.dropped(dropped.length);
                }
            }
            if(debug){
                if (pendingSysex.containsKey(key)) logger.info("Coalescing key:" +key);
            }
            pendingSysex.put(key, msg);
        }
        return true;
    }

    private void sendWithPacing(byte[] msg) throws Exception {
        int cost = msg.length;
        waitForTokens(cost);
        midiOut.sendMessage(msg);
        consumeTokens(cost);
        telemetry.sent(cost);
    }

    private void sendSysexChunked(byte[] full, int chunkSize, long gapNs) throws Exception {
        if (!isSysexFramed(full)) {
            sendWithPacing(full);
            return;
        }
        int pos = 1;
        int end = full.length - 1;
        while (pos < end) {
            int take = Math.min(chunkSize, end - pos);
            boolean last = (pos + take) >= end;
            byte[] chunk;
            if (pos == 1) {
                chunk = new byte[take + (last ? 2 : 1)];
                chunk[0] = (byte) 0xF0;
                System.arraycopy(full, pos, chunk, 1, take);
                if (last) chunk[chunk.length - 1] = (byte) 0xF7;
            } else if (last) {
                chunk = new byte[take + 1];
                System.arraycopy(full, pos, chunk, 0, take);
                chunk[chunk.length - 1] = (byte) 0xF7;
            } else {
                chunk = Arrays.copyOfRange(full, pos, pos + take);
            }
            sendWithPacing(chunk);
            pos += take;
            if (!last) LockSupport.parkNanos(gapNs);
        }
    }

    private void waitForTokens(int bytesNeeded) {
        for (;;) {
            refillTokens();
            int effectiveOutBps = getEffectiveOutBps();
            synchronized (tokenLock) {
                if (tokens >= bytesNeeded) return;

                if (effectiveOutBps <= 0) {
                    LockSupport.parkNanos(2_000_000L);
                    continue;
                }

                double deficit = bytesNeeded - tokens;
                long nanos = (long) ((deficit / effectiveOutBps) * 1_000_000_000L);
                LockSupport.parkNanos(Math.max(100_000L, nanos));
            }
        }
    }

    private void consumeTokens(int bytes) {
        synchronized (tokenLock) {
            tokens -= bytes;
            if (tokens < 0) tokens = 0;
        }
    }

    private void refillTokens() {
        long now = System.nanoTime();
        int effectiveOutBps = getEffectiveOutBps();

        synchronized (tokenLock) {
            long delta = now - lastRefillNs;
            if (delta <= 0) return;

            if (effectiveOutBps > 0) {
                double add = (delta / 1_000_000_000.0) * effectiveOutBps;
                tokens = Math.min(profile.burstBytes, tokens + add);
            } else {
                tokens = Math.min(profile.burstBytes, tokens);
            }

            lastRefillNs = now;
        }
    }


private int getEffectiveOutBps() {
    ThroughputProfile p = profile;
    if (p == ThroughputProfile.SAFE_DIN) {
        return p.bytesPerSecond;
    }

    double in;
    synchronized (tokenLock) {
        in = ingressBpsEwma;
    }

    int eff = (int) Math.floor(p.bytesPerSecond - in);
    return Math.max(0, eff);
}


    private void updateIngressRate(int byteCount) {
        long now = System.nanoTime();
        synchronized (tokenLock) {
            ingressBytesInWindow += byteCount;
            long elapsed = now - ingressWindowStartNs;
            if (elapsed < INGRESS_WINDOW_NS) return;

            double seconds = elapsed / 1_000_000_000.0;
            double bps = ingressBytesInWindow / seconds;

            ingressBpsEwma = (ingressBpsEwma == 0.0)
                    ? bps
                    : (ingressBpsEwma * (1.0 - INGRESS_EWMA_ALPHA) + bps * INGRESS_EWMA_ALPHA);

            ingressBytesInWindow = 0;
            ingressWindowStartNs = now;
        }
    }

    private static boolean isRealtime(byte[] msg) {
        if (msg.length == 0) return false;
        int b = msg[0] & 0xFF;
        return b >= 0xF8 && b <= 0xFF;
    }

    private static boolean isSysex(byte[] msg) {
        return msg.length > 0 && (msg[0] & 0xFF) == 0xF0;
    }

    private static boolean isSysexFramed(byte[] msg) {
        return msg.length >= 2
                && (msg[0] & 0xFF) == 0xF0
                && (msg[msg.length - 1] & 0xFF) == 0xF7;
    }

    @Override
    public void onBytesReceived(int byteCount) {
        telemetry.received(byteCount);
        updateIngressRate(byteCount);
    }

    public CoalesceEngine getCoalesceEngine(){
        return this.coalesceEngine;
    }

    public int getSysexQueueCapacityTotal() {
        return pendingSysexCapacity;
    }

    public int getSysexQueueDepth() {
        synchronized (sysexLock) {
            return pendingSysex.size();
        }
    }

    public int getSysexQueueRemaining() {
        synchronized (sysexLock) {
            int remaining = pendingSysexCapacity - pendingSysex.size();
            return Math.max(0, remaining);
        }
    }

    public int getSysexQueueFullPercent() {
        synchronized (sysexLock) {
            int used = pendingSysex.size();
            return (int) ((used * 100L) / pendingSysexCapacity);
        }
    }

    public int getSysexQueueRemainingPercent() {
        synchronized (sysexLock) {
            int remaining = pendingSysexCapacity - pendingSysex.size();
            if (remaining <= 0) return 0;
            return (int) ((remaining * 100L) / pendingSysexCapacity);
        }
    }

}