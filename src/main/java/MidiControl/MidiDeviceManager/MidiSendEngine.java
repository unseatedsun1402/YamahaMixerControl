package MidiControl.MidiDeviceManager;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Logger;

public final class MidiSendEngine {
    
    // Pacing profiles are orthogonal to your logical TransportMode.
    public enum ThroughputProfile {
        SAFE_DIN   (3125, 128, 1_500_000L, 4l, 98),  // bytes/s, sysexChunk, interChunkGapNs
        FAST_USB   (40000, 512,   200_000L, 2l, 512),
        FAST_RTP   (80000, 1024,   50_000L, 2l, 1024);

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
    private final ExecutorService worker;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile ThroughputProfile profile = ThroughputProfile.SAFE_DIN;

    // Token bucket (in bytes)
    private final Object tokenLock = new Object();
    private double tokens;
    private long lastRefillNs;

    // constructor
    public MidiSendEngine(MidiOutput out, int queueCapacity) {
        this.midiOut = out;
        this.normalLane   = new ArrayBlockingQueue<>(queueCapacity);
        this.realtimeLane = new ArrayBlockingQueue<>(128);
        this.worker = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "midi-send-worker");
            t.setDaemon(true);
            return t;
        });
        this.tokens = 0.0;
        this.lastRefillNs = System.nanoTime();
    }

    public void setThroughputProfile(ThroughputProfile p) {
        this.profile = p;
        log.info("Throughput profile set to " + p);
    }

    public ThroughputProfile getThroughputProfile() {
        return this.profile;
    }

    public boolean offer(byte[] msg) {
        if (isRealtime(msg)) return realtimeLane.offer(msg);
        return normalLane.offer(msg);
    }

    public void start() {
        if (!running.compareAndSet(false, true)) return;
        worker.execute(this::runLoop);
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) return;
        worker.shutdownNow();
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

    private byte[] pollNext() throws InterruptedException {
        byte[] rt = realtimeLane.poll();
        if (rt != null) return rt;
        byte[] normal = normalLane.poll(250, TimeUnit.MICROSECONDS);
        if (normal != null) return normal;
        return realtimeLane.poll();
    }

    private void sendWithPacing(byte[] msg) throws Exception {
        int cost = msg.length;            // wire bytes (simple cost)
        waitForTokens(cost);
        midiOut.sendMessage(msg);
        consumeTokens(cost);
    }

    private void sendSysexChunked(byte[] full, int chunkSize, long gapNs) throws Exception {
        if (full.length < 2 || (full[0] & 0xFF) != 0xF0 || (full[full.length-1] & 0xFF) != 0xF7) {
            sendWithPacing(full); // non‑canonical SysEx, just pace
            return;
        }
        int pos = 1, end = full.length - 1; // exclude F0/F7
        while (pos < end) {
            int take = Math.min(chunkSize, end - pos);
            boolean last = (pos + take) >= end;
            byte[] chunk;
            if (pos == 1) {
                if (last) {
                    chunk = new byte[take + 2];
                    chunk[0] = (byte)0xF0;
                    System.arraycopy(full, pos, chunk, 1, take);
                    chunk[chunk.length - 1] = (byte)0xF7;
                } else {
                    chunk = new byte[take + 1];
                    chunk[0] = (byte)0xF0;
                    System.arraycopy(full, pos, chunk, 1, take);
                }
            } else if (last) {
                chunk = new byte[take + 1];
                System.arraycopy(full, pos, chunk, 0, take);
                chunk[chunk.length - 1] = (byte)0xF7;
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
            synchronized (tokenLock) {
                if (tokens >= bytesNeeded) return;
                double deficit = bytesNeeded - tokens;
                long nanos = (long)((deficit / profile.bytesPerSecond) * 1_000_000_000L);
                LockSupport.parkNanos(Math.max(100_000L, nanos)); // ≥0.1 ms
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
        long delta = now - lastRefillNs;
        if (delta <= 0) return;
        double add = (delta / 1_000_000_000.0) * profile.bytesPerSecond;
        synchronized (tokenLock) {
            tokens = Math.min(profile.burstBytes, tokens + add);
            lastRefillNs = now;
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
}