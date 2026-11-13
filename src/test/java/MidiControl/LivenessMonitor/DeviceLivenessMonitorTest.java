package MidiControl.LivenessMonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ConcurrentLinkedQueue;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;
import MidiControl.DeskDiscovery.DeskDiscovery;
import MidiControl.MidiDeviceManager.MidiIOManager;
import MidiControl.MidiDeviceManager.MidiInput;
import MidiControl.MidiDeviceManager.MidiInputReceiver;
import MidiControl.Mocks.MockCanonicalRegistry;
import MidiControl.Routing.WebSocketEndpoint;

class DeviceLivenessMonitorTest {
    private static class FakeInput implements MidiInput {
        final MidiInputReceiver receiver = new MidiInputReceiver(new ConcurrentLinkedQueue<>());
        boolean open = true;

        @Override public void setReceiver(Receiver receiver) {}
        @Override public void close() { open = false; receiver.close(); }
        @Override public MidiDevice.Info getDeviceInfo() { return null; }
        @Override public boolean isOpen() { return open; }
        @Override public MidiInputReceiver getInputReceiver() { return receiver; }
    }

    private static class FakeIo extends MidiIOManager {
        MidiInput input;
        int resets;
        @Override public MidiInput getMidiIn() { return input; }
        @Override public boolean resetMidiSubsystem() { resets++; return true; }
    }

    private static class FakeDiscovery extends DeskDiscovery {
        int probes;
        Runnable duringProbe = () -> {};
        @Override public boolean probeForLiveness(String model) {
            probes++;
            duringProbe.run();
            return false;
        }
    }

    @Test
    void recentInputCountsAsConnectedWithoutProbing() throws Exception {
        FakeInput input = new FakeInput();
        FakeIo io = new FakeIo();
        io.input = input;
        FakeDiscovery discovery = new FakeDiscovery();
        MockCanonicalRegistry registry = new MockCanonicalRegistry();
        registry.setDeskType("YAMAHA_M7CL");
        DeviceLivenessMonitor monitor = new DeviceLivenessMonitor(discovery, registry, io);
        try {
            input.receiver.send(new ShortMessage(0xFE), -1);
            monitor.checkLiveness();
            assertEquals(0, discovery.probes);
            assertEquals(0, io.resets);
            assertTrue(isConnected());
            assertFalse(input.receiver.hasRecentInput(0));
        } finally {
            input.close();
            monitor.stopMonitoring();
        }
    }

    @Test
    void inputDuringFailedProbePreventsDisconnectionAndReset() throws Exception {
        FakeInput input = new FakeInput();
        FakeIo io = new FakeIo();
        io.input = input;
        FakeDiscovery discovery = new FakeDiscovery();
        MockCanonicalRegistry registry = new MockCanonicalRegistry();
        registry.setDeskType("YAMAHA_M7CL");
        DeviceLivenessMonitor monitor = new DeviceLivenessMonitor(discovery, registry, io);
        ShortMessage message = new ShortMessage(0xFE);
        try {
            monitor.checkLiveness();
            monitor.checkLiveness();
            discovery.duringProbe = () -> input.receiver.send(message, -1);
            monitor.checkLiveness();
            assertEquals(3, discovery.probes);
            assertEquals(0, io.resets);
            assertTrue(isConnected());
            input.close();
            monitor.checkLiveness();
            monitor.checkLiveness();
            assertEquals(0, io.resets, "Successful input must clear previous failures");
            monitor.checkLiveness();
            assertEquals(1, io.resets);
        } finally {
            input.close();
            monitor.stopMonitoring();
        }
    }

    @Test
    void closedOrReplacedInputDoesNotKeepDeskAlive() throws Exception {
        FakeInput oldInput = new FakeInput();
        FakeIo io = new FakeIo();
        io.input = oldInput;
        FakeDiscovery discovery = new FakeDiscovery();
        MockCanonicalRegistry registry = new MockCanonicalRegistry();
        registry.setDeskType("YAMAHA_M7CL");
        DeviceLivenessMonitor monitor = new DeviceLivenessMonitor(discovery, registry, io);
        try {
            oldInput.receiver.send(new ShortMessage(0xFE), -1);
            oldInput.close();
            monitor.checkLiveness();
            io.input = new FakeInput();
            monitor.checkLiveness();
            monitor.checkLiveness();
            assertEquals(3, discovery.probes);
            assertEquals(1, io.resets);
            assertFalse(isConnected());
        } finally {
            io.input.close();
            monitor.stopMonitoring();
        }
    }

    private static boolean isConnected() {
        return JsonParser.parseString(WebSocketEndpoint.lastSent).getAsJsonObject()
                .getAsJsonObject("payload").getAsJsonObject("details")
                .get("isConnected").getAsBoolean();
    }
}
