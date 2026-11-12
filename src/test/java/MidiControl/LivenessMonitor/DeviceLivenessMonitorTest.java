package MidiControl.LivenessMonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import MidiControl.DeskDiscovery.DeskDiscovery;
import MidiControl.MidiDeviceManager.MidiIOManager;
import MidiControl.Mocks.MockCanonicalRegistry;
import MidiControl.Routing.WebSocketEndpoint;
import com.google.gson.JsonParser;

class DeviceLivenessMonitorTest {
    @Test
    void incompleteDevicePairIsNotProbedOrReset() {
        class FakeIo extends MidiIOManager {
            boolean valid;
            int resets;
            @Override public boolean hasValidDevices() { return valid; }
            @Override public boolean resetMidiSubsystem() { resets++; return true; }
        }
        class FakeDiscovery extends DeskDiscovery {
            int probes;
            String lastModel;
            @Override public boolean probeForLiveness(String model) { probes++; lastModel = model; return false; }
        }
        FakeIo io = new FakeIo();
        FakeDiscovery discovery = new FakeDiscovery();
        MockCanonicalRegistry registry = new MockCanonicalRegistry();
        registry.setDeskType("YAMAHA_01V96I");
        DeviceLivenessMonitor monitor = new DeviceLivenessMonitor(discovery, registry, io);
        try {
            MockCanonicalRegistry newRegistry = new MockCanonicalRegistry();
            newRegistry.setDeskType("YAMAHA_M7CL");
            monitor.injectNewRegistry(newRegistry);
            for (int i = 0; i < 4; i++) monitor.checkLiveness();
            assertEquals(0, discovery.probes);
            assertEquals(0, io.resets);
            assertEquals("Desk YAMAHA_M7CL  is not connected",
                    JsonParser.parseString(WebSocketEndpoint.lastSent).getAsJsonObject()
                            .getAsJsonObject("payload").get("message").getAsString());

            io.valid = true;
            for (int i = 0; i < 3; i++) monitor.checkLiveness();
            assertEquals(3, discovery.probes);
            assertEquals("YAMAHA_M7CL", discovery.lastModel);
            assertEquals(1, io.resets);
        } finally {
            monitor.stopMonitoring();
            io.shutdown();
        }
    }
}
