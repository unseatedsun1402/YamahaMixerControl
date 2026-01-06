package MidiControl.Server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import MidiControl.Controls.ControlInstance;
import MidiControl.Controls.SourceAllInstances;
import MidiControl.Routing.OutputRequestSender;

public class RehydrationManager {

    private final OutputRequestSender outputRouter;
    private final SourceAllInstances registry;
    private final ScheduledExecutorService scheduler;

    private final Map<String, Long> pending = new ConcurrentHashMap<>();
    private static final long TIMEOUT_MS = 350;
    private static final Logger logger = Logger.getLogger(RehydrationManager.class.getName());

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

    /**
     * Attach listeners to every ControlInstance in the registry.
     * The registry owns the structure; we simply enumerate it.
     */
    private void attachListeners() {
        for (ControlInstance ci : registry.getAllInstances()) {
            ci.addListener((instance, newValue) -> {
                onControlUpdated(instance.getCanonicalId());
            });
        }
    }

    /**
     * Request a single control value from the desk.
     */
    public void request(String canonicalId) {
        logger.info("Rehydration request: " + canonicalId);
        pending.put(canonicalId, System.currentTimeMillis());
        outputRouter.applyRequest(canonicalId);

        scheduler.schedule(() -> checkTimeout(canonicalId),
                TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Request all control values from the desk.
     */
    public void rehydrateAll() {
        List<ControlInstance> all = new ArrayList<>(registry.getAllInstances());

        all.sort((a, b) -> Integer.compare(a.getPriority(), b.getPriority()));

        Iterator<ControlInstance> it = all.iterator();

        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                if (!it.hasNext()) {
                    return; // done
                }

                ControlInstance next = it.next();
                int prio = normalizePriority(next.getPriority());

                int batch = switch (prio) {
                    case 1 -> 1;   // faders, ON, SOLO
                    case 2 -> 2;   // level/gain/pan
                    case 3 -> 4;   // deep EQ/dynamics
                    default -> 8;  // very deep / rarely used
                };

                long delay = switch (prio) {
                    case 1 -> 10;
                    case 2 -> 15;
                    case 3 -> 40;
                    default -> 50;
                };

                request(next.getCanonicalId());

                for (int i = 1; i < batch && it.hasNext(); i++) {
                    ControlInstance ci = it.next();
                    if (normalizePriority(ci.getPriority()) != prio) {
                        break;
                    }
                    request(ci.getCanonicalId());
                }

                scheduler.schedule(this, delay, TimeUnit.MILLISECONDS);
            }
        }, 0, TimeUnit.MILLISECONDS);
    }

    private int normalizePriority(int p) {
        if (p <= 1) return 1;
        if (p == 2) return 2;
        if (p == 3) return 3;
        return 4;
    }


    /**
     * Called when a ControlInstance receives a value from the desk.
     */
    public void onControlUpdated(String canonicalId) {
        Long expected = pending.remove(canonicalId);
        if (expected == null) {
            // Not a rehydration response — just process normally
            logger.warning("Not a rehydration response or timed out:" + canonicalId);
            return;
        }
        logger.fine("Rehydrated "+canonicalId + " control value");
    }

    /**
     * Timeout handler.
     */
    private void checkTimeout(String canonicalId) {
        if (pending.containsKey(canonicalId)) {
            pending.remove(canonicalId);
            logger.warning("Rehydration timeout for " + canonicalId);
        }
    }

    public Boolean isPending(String string) {
        return this.pending.containsKey(string);
    }

    public void clearPending() {
        pending.clear();
    }

}
