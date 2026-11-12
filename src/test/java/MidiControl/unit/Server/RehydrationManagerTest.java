package MidiControl.unit.Server;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.ControlInstance;
import MidiControl.Controls.SubControl;
import MidiControl.Mocks.MockCanonicalRegistry;
import MidiControl.Routing.OutputRequestSender;
import MidiControl.Server.Rehydration.RehydrationListener;
import MidiControl.Server.Rehydration.RehydrationManager;
import MidiControl.Server.MidiProcessingLoop;
import MidiControl.ControlServer.HardwareInputHandler;
import MidiControl.NrpnUtils.NrpnParser;
import MidiControl.NrpnUtils.NrpnRegistry;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;
import MidiControl.UserInterface.Frontend.GuiBroadcastListener;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.SysexMessage;

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
    void probeReadsMidiChannelFromProcessedSysex() {
        var mappings = SysexMappingLoader.loadMappingsFromResource("MidiControl/m7cl_sysex_mappings.json");
        CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));
        ControlInstance fader = registry.resolveCanonicalId("kInputFader.kFader.0");
        ConcurrentLinkedQueue<MidiMessage> input = new ConcurrentLinkedQueue<>();
        MidiProcessingLoop loop = new MidiProcessingLoop(input,
                new HardwareInputHandler(new NrpnParser(), new NrpnRegistry()), registry,
                new GuiBroadcastListener((json, context) -> {}, id -> null));
        FakeOutputRouter router = new FakeOutputRouter() {
            @Override public void send(byte[] request) {
                super.send(request);
                try {
                    for (int channel : new int[] {6, 7}) {
                        byte[] bytes = fader.getSysex().buildChangeMessage(500, 0, channel);
                        SysexMessage response = new SysexMessage();
                        response.setMessage(bytes, bytes.length);
                        input.add(response);
                        loop.processIncomingMidi();
                    }
                } catch (javax.sound.midi.InvalidMidiDataException e) {
                    throw new AssertionError(e);
                }
            }
        };
        ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);
        RehydrationManager manager = new RehydrationManager(router, registry, scheduler);
        try {
            AtomicInteger detectedChannel = new AtomicInteger(-1);
            manager.probe(fader.getCanonicalId(), 300, 7,
                    (instance, channel) -> detectedChannel.set(channel));
            assertEquals(7, router.lastSent[2] & 0x0F);
            assertEquals(7, detectedChannel.get());
            assertEquals(0, manager.getOutstandingRequests());
        } finally {
            manager.shutdown();
        }
    }

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

        CappedImmediateScheduler scheduler = new CappedImmediateScheduler(5);

        RehydrationManager mgr = new RehydrationManager(fakeRouter, fakeRegistry, scheduler);

        mgr.request("foo.bar.0");

        assertFalse(mgr.isPending("foo.bar.0"));
    }

    @Test
    void rehydrateAll_requestsEachControlInstance() throws InterruptedException {
        FakeOutputRouter router = new FakeOutputRouter();

        ControlInstance c1 = makeInstance("kTestGroupA", "kTestSub", 0, 1);
        ControlInstance c2 = makeInstance("kTestGroupB", "kTestSub", 0, 3);

        CanonicalRegistry reg = new MockCanonicalRegistry() {
            @Override
            public Collection<ControlInstance> getAllInstances() {
                return List.of(c1, c2);
            }
        };

        CappedImmediateScheduler scheduler = new CappedImmediateScheduler(50);

        RehydrationManager mgr = new RehydrationManager(router, reg, scheduler);

        FakeListener listener = new FakeListener();

        mgr.rehydrateAll(listener);
        
        Thread.sleep(210);

        assertEquals(2, router.callCount);
        assertEquals(1, listener.finished);

        // Because FakeOutputRouter only keeps the lastRequestedId, we can at least assert it ends on c2.
        assertEquals("kTestGroupB.kTestSub.0", router.lastRequestedId);
    }

        @Test
    void testRehydrateAllSkipsP4() throws InterruptedException {
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

        CappedImmediateScheduler scheduler = new CappedImmediateScheduler(50);

        RehydrationManager mgr = new RehydrationManager(router, reg, scheduler);

        FakeListener listener = new FakeListener();

        mgr.rehydrateAll(listener);

        Thread.sleep(210);

        assertEquals(1, router.callCount);
        assertEquals(1, listener.finished);
        
        assertEquals("kTestGroupA.kTestSub.0", router.lastRequestedId);
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

    @Test
    void shutdown_stopsScheduler() {
        ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);
        RehydrationManager mgr = new RehydrationManager(
            new FakeOutputRouter(),
            new MockCanonicalRegistry(),
            scheduler
        );

        mgr.shutdown();

        assertTrue(scheduler.isShutdown());
    }

    @Test
    void profileReloadDiscardsOldMeterSelection() {
        FakeOutputRouter router = new FakeOutputRouter();
        MockCanonicalRegistry m7cl = new MockCanonicalRegistry();
        m7cl.setDeskType("YAMAHA_M7CL");
        MockCanonicalRegistry v96 = new MockCanonicalRegistry();
        v96.setDeskType("YAMAHA_01V96I");
        RehydrationManager manager = new RehydrationManager(router, m7cl, new NoOpScheduler());
        try {
            manager.requestMeters(0, 7, 47, 1);
            manager.injectNewRegistry(v96);
            manager.requestMeters();
            assertArrayEquals(new byte[] {
                (byte) 0xF0, 0x43, 0x30, 0x3E, 0x1A, 0x21,
                0, 0, 0, 0, 32, (byte) 0xF7
            }, router.lastSent);
            manager.injectNewRegistry(m7cl);
            manager.requestMeters();
            assertEquals(0x11, router.lastSent[4]);
            assertEquals(0, router.lastSent[7]);
            assertEquals(48, router.lastSent[10]);
        } finally {
            manager.shutdown();
        }
    }

    @Test
    void registryReplacementDetachesOldControlListeners() {
        ControlInstance oldControl = makeInstance("group", "control", 0, 1);
        ControlInstance newControl = makeInstance("group", "control", 0, 1);
        CanonicalRegistry oldRegistry = new MockCanonicalRegistry() {
            @Override public Collection<ControlInstance> getAllInstances() { return List.of(oldControl); }
        };
        CanonicalRegistry newRegistry = new MockCanonicalRegistry() {
            @Override public Collection<ControlInstance> getAllInstances() { return List.of(newControl); }
        };
        RehydrationManager manager =
                new RehydrationManager(new FakeOutputRouter(), oldRegistry, new NoOpScheduler());
        try {
            manager.injectNewRegistry(newRegistry);
            manager.request(oldControl.getCanonicalId());
            oldControl.updateValue(1);
            assertTrue(manager.isPending(oldControl.getCanonicalId()));
            newControl.updateValue(1);
            assertFalse(manager.isPending(oldControl.getCanonicalId()));
        } finally {
            manager.shutdown();
        }
    }

    @Test
    void meterRequestUsesDiscoveredMidiChannel() {
        var mappings = MidiControl.Mocks.FakeSysexMapping.fakeSysexMapping();
        mappings.get(0).setDeviceNumber(7);
        CanonicalRegistry registry = new CanonicalRegistry(
                mappings, new MidiControl.SysexUtils.SysexParser(mappings));
        registry.setDeskType("YAMAHA_M7CL");
        FakeOutputRouter output = new FakeOutputRouter();
        RehydrationManager manager = new RehydrationManager(output, registry, new NoOpScheduler());
        try {
            manager.requestMeters();
            assertEquals(0x37, output.lastSent[2] & 0xFF);
        } finally {
            manager.shutdown();
        }
    }
}
