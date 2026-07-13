package MidiControl.unit.Server;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.ControlInstance;
import MidiControl.Controls.SourceAllInstances;
import MidiControl.Controls.SubControl;
import MidiControl.Mocks.MockCanonicalRegistry;
import MidiControl.Routing.OutputRequestSender;
import MidiControl.Server.Rehydration.RehydrationListener;
import MidiControl.Server.Rehydration.RehydrationManager;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class RehydrationManagerTest {

    // ---------------- Fakes ----------------

    static class FakeOutputRouter implements OutputRequestSender {
        String lastRequestedId = null;
        int callCount = 0;

        byte[] lastSent = null;
        int sendCount = 0;

        @Override
        public void applyRequest(String canonicalId) {
            this.lastRequestedId = canonicalId;
            this.callCount++;
        }

        @Override
        public void send(byte[] message) {
            this.lastSent = message;
            this.sendCount++;
        }
    }

    /**
     * Scheduler that does nothing (so TIMEOUT won't run).
     * This matches your existing approach for tests that only want to observe request() behaviour.
     */
    static class NoOpScheduler extends ScheduledThreadPoolExecutor {
        NoOpScheduler() { super(1); }

        @Override
        public ScheduledFuture<?> schedule(Runnable r, long delay, TimeUnit unit) {
            // intentionally do nothing
            return null;
        }
    }

    /**
     * Scheduler that runs scheduled tasks immediately, but only up to a cap.
     * Used to drive rehydrateAll() without real timing.
     */
    static class CappedImmediateScheduler extends ScheduledThreadPoolExecutor {
        private int runs = 0;
        private final int cap;

        CappedImmediateScheduler(int cap) {
            super(1);
            this.cap = cap;
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable r, long delay, TimeUnit unit) {
            if (runs++ < cap) {
                r.run();
            }
            return null;
        }
    }

    static class FakeListener implements RehydrationListener {
        int finished = 0;
        int reset = 0;

        @Override public void onFinished() { finished++; }
        @Override public void onReset() { reset++; }
    }

    // ---------------- Helpers ----------------

    /**
     * Build a *real* composite: ControlGroup -> SubControl -> ControlInstance.
     * This ensures ControlInstance constructs its canonical ID correctly.
     */
    private ControlInstance makeInstance(String groupName, String subName, int index, int priority) {
        ControlGroup g = new ControlGroup(groupName);
        SubControl sc = new SubControl(g, subName);
        ControlInstance ci = new ControlInstance(sc, index, null, null);
        ci.setPriority(priority);
        return ci;
    }

    // ---------------- Tests ----------------

    @Test
    void request_callsOutputRouterApplyRequest() {
        FakeOutputRouter fakeRouter = new FakeOutputRouter();
        CanonicalRegistry fakeRegistry = new MockCanonicalRegistry();
        NoOpScheduler fakeScheduler = new NoOpScheduler();

        RehydrationManager mgr = new RehydrationManager(fakeRouter, fakeRegistry, fakeScheduler);

        mgr.request("kInputAUX.kAUX1Level.0");

        assertEquals("kInputAUX.kAUX1Level.0", fakeRouter.lastRequestedId);
        assertEquals(1, fakeRouter.callCount);
    }

    @Test
    void request_addsCanonicalIdToPending() {
        FakeOutputRouter fakeRouter = new FakeOutputRouter();
        CanonicalRegistry fakeRegistry = new MockCanonicalRegistry();
        NoOpScheduler fakeScheduler = new NoOpScheduler();

        RehydrationManager mgr = new RehydrationManager(fakeRouter, fakeRegistry, fakeScheduler);

        mgr.request("foo.bar.0");

        assertTrue(mgr.isPending("foo.bar.0"));
    }

    @Test
    void onControlUpdated_clearsPending() {
        FakeOutputRouter fakeRouter = new FakeOutputRouter();
        CanonicalRegistry fakeRegistry = new MockCanonicalRegistry();
        NoOpScheduler fakeScheduler = new NoOpScheduler();

        RehydrationManager mgr = new RehydrationManager(fakeRouter, fakeRegistry, fakeScheduler);

        mgr.request("foo.bar.0");
        mgr.onControlUpdated("foo.bar.0");

        assertFalse(mgr.isPending("foo.bar.0"));
    }

    @Test
    void timeout_removesPending() {
        FakeOutputRouter fakeRouter = new FakeOutputRouter();
        CanonicalRegistry fakeRegistry = new MockCanonicalRegistry();

        // Immediate schedule => timeout check runs immediately and removes pending
        CappedImmediateScheduler scheduler = new CappedImmediateScheduler(5);

        RehydrationManager mgr = new RehydrationManager(fakeRouter, fakeRegistry, scheduler);

        mgr.request("foo.bar.0");

        assertFalse(mgr.isPending("foo.bar.0"));
    }

    @Test
    void rehydrateAll_requestsEachControlInstance() {
        FakeOutputRouter router = new FakeOutputRouter();

        // Build real composite instances so canonical IDs are valid:
        // "kTestGroupA.kTestSub.0" etc.
        ControlInstance c1 = makeInstance("kTestGroupA", "kTestSub", 0, 1);
        ControlInstance c2 = makeInstance("kTestGroupB", "kTestSub", 0, 4);

        CanonicalRegistry reg = new MockCanonicalRegistry() {
            @Override
            public Collection<ControlInstance> getAllInstances() {
                return List.of(c1, c2);
            }
        };

        // Cap high enough to allow the initial run + reschedules to drain 2 items and finish
        CappedImmediateScheduler scheduler = new CappedImmediateScheduler(50);

        RehydrationManager mgr = new RehydrationManager(router, reg, scheduler);

        FakeListener listener = new FakeListener();

        mgr.rehydrateAll(listener);

        assertEquals(2, router.callCount);
        assertEquals(1, listener.finished);

        // The weighted pattern should select priority-1 first when available
        // so the first request should be c1's canonicalId.
        // Because FakeOutputRouter only keeps the lastRequestedId, we can at least assert it ends on c2.
        assertEquals("kTestGroupB.kTestSub.0", router.lastRequestedId);
    }

    @Test
    void clearPending_clearsAndNotifiesListener() {
        FakeOutputRouter fakeRouter = new FakeOutputRouter();
        CanonicalRegistry fakeRegistry = new MockCanonicalRegistry();
        NoOpScheduler fakeScheduler = new NoOpScheduler();

        RehydrationManager mgr = new RehydrationManager(fakeRouter, fakeRegistry, fakeScheduler);

        mgr.request("foo.bar.0");
        assertTrue(mgr.isPending("foo.bar.0"));

        FakeListener listener = new FakeListener();
        mgr.clearPending(listener);

        assertFalse(mgr.isPending("foo.bar.0"));
        assertEquals(1, listener.reset);
    }
}