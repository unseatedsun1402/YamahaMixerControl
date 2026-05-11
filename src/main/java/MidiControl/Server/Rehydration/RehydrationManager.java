package MidiControl.Server.Rehydration;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Logger;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;
import MidiControl.Controls.SourceAllInstances;
import MidiControl.MidiDeviceManager.ServerSettings;
import MidiControl.Routing.OutputRequestSender;
import MidiControl.SysexUtils.ModelNumbers;
import MidiControl.UserInterface.Meter.MeterRequest;

public class RehydrationManager{

    private final OutputRequestSender outputRouter;
    private SourceAllInstances registry;
    private final ScheduledExecutorService scheduler;

    private final Map<String, Long> pending = new ConcurrentHashMap<>();
    private static final long TIMEOUT_MS = 200;

    // Metrics counters and configuration values
    private final AtomicInteger outstandingTransactions = new AtomicInteger(0);
    private final AtomicInteger timedOutTransactions = new AtomicInteger(0);
    private final AtomicInteger periodTimedOutTransactions = new AtomicInteger(0);
    private final LongAdder totalRttMs = new LongAdder();
    private final AtomicInteger completedRequests = new AtomicInteger(0);
    private static final int MAX_TIMEOUT_COUNT = 10_000;
    private final RehydrationTelemetry telemetry = new RehydrationTelemetry(this);


    private static final Logger logger = Logger.getLogger(RehydrationManager.class.getName());
    private static byte[] cachedMeterRequest = null;
    private static byte cachedModelNumber;
    private static boolean debug = true;
    private static long BASE_DELAY_MS = 4L;
    private static boolean running = false;

    public RehydrationManager(OutputRequestSender outputRouter,
                              SourceAllInstances registry,
                              ScheduledExecutorService scheduler) {
        this.outputRouter = outputRouter;
        this.registry = registry;
        this.scheduler = scheduler;

        attachListeners();
    }
    
    public RehydrationManager() {
        this.outputRouter = null;
        this.registry = null;
        this.scheduler = null;
    }

    public static void enableDebug(){
        debug = true;
    }

    private void attachListeners() {
        for (ControlInstance ci : registry.getAllInstances()) {
            ci.addListener((instance, newValue) -> {
                onControlUpdated(instance.getCanonicalId());
            });
        }
    }

    public void request(String canonicalId) {
        pending.put(canonicalId, System.currentTimeMillis());
        outstandingTransactions.incrementAndGet();
        outputRouter.applyRequest(canonicalId);

        scheduler.schedule(() -> checkTimeout(canonicalId),
                TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    public void rehydrateAll(RehydrationListener listener) {
        final long rehydrationStart = System.currentTimeMillis();

        final List<ControlInstance> all = new ArrayList<>(registry.getAllInstances());
        all.sort((a, b) -> Integer.compare(a.getPriority(), b.getPriority()));

        // ---- Weighted Rnd Rbn Configuration ----
        final int[] WEIGHTED_PATTERN = new int[] {
            1, 1, 1, 1,
            2, 2, 2,
            3, 3,
            4
        };

        final ArrayDeque<ControlInstance> q1 = new ArrayDeque<>();
        final ArrayDeque<ControlInstance> q2 = new ArrayDeque<>();
        final ArrayDeque<ControlInstance> q3 = new ArrayDeque<>();
        final ArrayDeque<ControlInstance> q4 = new ArrayDeque<>();

        for (ControlInstance ci : all) {
            switch (normalizePriority(ci.getPriority())) {
                case 1 -> q1.add(ci);
                case 2 -> q2.add(ci);
                case 3 -> q3.add(ci);
                default -> q4.add(ci);
            }
        }

        final AtomicInteger remaining = new AtomicInteger(all.size());
        final AtomicInteger patternIdx = new AtomicInteger(0);

        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                if(!running)running = true;
                if (remaining.get() <= 0) {
                    logger.info("Rehydration complete: all priorities processed.");
                    all.clear();
                    if (debug) {
                        logger.info("Rehydration finished in: " + (int)((System.currentTimeMillis() - rehydrationStart)/1000) + " s"); 
                    }
                    listener.onFinished();
                    running = false;
                    return;
                }

                ControlInstance selected = null;

                int probes = 0;
                while (probes < WEIGHTED_PATTERN.length && selected == null) {
                    int p = WEIGHTED_PATTERN[patternIdx.getAndUpdate(i -> (i + 1) % WEIGHTED_PATTERN.length)];
                    selected = pollFromQueue(p);
                    probes++;
                }

                if (selected == null) {
                    selected = pollFromQueue(1);
                    if (selected == null) selected = pollFromQueue(2);
                    if (selected == null) selected = pollFromQueue(3);
                    if (selected == null) selected = pollFromQueue(4);
                }

                if (selected == null) {
                    remaining.set(0);
                    logger.info("Rehydration complete: all priorities processed.");
                    all.clear();
                    if (debug) {
                        logger.info("Rehydration finished in: " + (int)((System.currentTimeMillis() - rehydrationStart)/1000) + " seconds");
                    }
                    return;
                }

                request(selected.getCanonicalId());
                remaining.decrementAndGet();

                scheduler.schedule(this, BASE_DELAY_MS, TimeUnit.MILLISECONDS);
            }

            private ControlInstance pollFromQueue(int normalizedPrio) {
                return switch (normalizedPrio) {
                    case 1 -> q1.pollFirst();
                    case 2 -> q2.pollFirst();
                    case 3 -> q3.pollFirst();
                    default -> q4.pollFirst();
                };
            }
        }, 0, TimeUnit.MILLISECONDS);
    }

    public void onControlUpdated(String canonicalId) {
        Long expected = pending.remove(canonicalId);
        if (expected == null) {
            logger.warning(canonicalId + " was not expected or no longer pending i.e. already recieved or timedout and removed");
            return;
        }

        long rtt = System.currentTimeMillis() - expected;
        totalRttMs.add(rtt);
        completedRequests.incrementAndGet();
        outstandingTransactions.decrementAndGet();
    }

    public static boolean isRunning(){ return running; }

    
    public int getOutstandingRequests() {return outstandingTransactions.get();}
    public int getTimedOutRequestsTotal() {return timedOutTransactions.get();}

    private int normalizePriority(int p) {
        if (p <= 1) return 1;
        if (p == 2) return 2;
        if (p == 3) return 3;
        return 4;
    }


    private void checkTimeout(String canonicalId) {
        if (pending.remove(canonicalId) != null) {
            outstandingTransactions.decrementAndGet();
            incrementRequestTimeoutCounter();
        }
    }

    private void incrementRequestTimeoutCounter() {
        int v;
        do {
            v = timedOutTransactions.get();
            if (v >= MAX_TIMEOUT_COUNT) return;
        } while (!timedOutTransactions.compareAndSet(v, v + 1));

        periodTimedOutTransactions.incrementAndGet();
    }

    public Boolean isPending(String string) {
        return this.pending.containsKey(string);
    }

    public void clearPending(RehydrationListener listener){
        int cleared = pending.size();
        pending.clear();
        outstandingTransactions.addAndGet(-cleared);
        listener.onReset();
    }

    public synchronized void requestMeters() {
        byte checkModelNumber = ModelNumbers.getModelByteByString(
                new ServerSettings().getConsoleName()
        );

        if (checkModelNumber < 0 || checkModelNumber > 127) {
            logger.warning("Cannot make a request, the model number is invalid " + checkModelNumber);
            return;
        }

        if (cachedMeterRequest == null || checkModelNumber != cachedModelNumber) {
            cachedMeterRequest = buildRequestBytes(checkModelNumber).clone();
        }

        outputRouter.send(cachedMeterRequest);
    }

    private byte[] buildRequestBytes(byte modelNumber) {
        cachedModelNumber = modelNumber;
        MeterRequest request = new MeterRequest(0, modelNumber, 0x0, 0x0);
        request.setChannelCount(32);
        request.setStartChannel(0);
        return request.toByteArray();
    }

    public static void changeRehydrationDelay(long newDelay){
        if(newDelay > 0 & newDelay < 10) {
            BASE_DELAY_MS = newDelay;
            logger.info("Rehydration poll rate set to: "+BASE_DELAY_MS+"ms");
        }
    }

    public int getPeriodTimedOutRequestsTotal() {return periodTimedOutTransactions.getAndSet(0);}

    public long getAvgRequestRttMsAndReset() {
        int count = completedRequests.getAndSet(0);
        if (count == 0) {
            totalRttMs.reset();
            return 0;
        }
        long total = totalRttMs.sumThenReset();
        return total / count;
    }

    public RehydrationTelemetry getRehydrationTelemetry(){
        return telemetry;
    }

    public void injectNewRegistry(CanonicalRegistry newRegistry) {
        this.registry = newRegistry;
        attachListeners();
    }
}