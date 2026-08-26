package MidiControl.functional.Control;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.SysexMessage;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import MidiControl.Controls.ControlInstance;
import MidiControl.MidiDeviceManager.DeskDiscovery;
import MidiControl.MidiDeviceManager.DeskDiscoveryResult;
import MidiControl.MidiDeviceManager.MidiDeviceDTO;
import MidiControl.Mocks.MockCanonicalRegistry;
import MidiControl.Mocks.MockMidiDevice;
import MidiControl.Mocks.MockMidiIOManager;
import MidiControl.Routing.OutputRequestSender;
import MidiControl.Server.Rehydration.RehydrationManager;
import MidiControl.SysexUtils.SysexMapping;

public class DiscoveryTest {

    private static final class TestMidiDeviceInfo extends javax.sound.midi.MidiDevice.Info {
        private TestMidiDeviceInfo(String name, String vendor, String description, String version) {
            super(name, vendor, description, version);
        }
    }

    private String bytesToHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) sb.append(String.format("%02X  ", b));
        return sb.toString();
    }

    /** Simulated Yamaha desk using real SysexMapping */
    public class SimulatedDeskReceiver implements Receiver {

        private final SysexMapping mapping;
        private Consumer<byte[]> inboundHandler;

        public SimulatedDeskReceiver(SysexMapping mapping) {
            this.mapping = mapping;
        }

        public void setInboundHandler(Consumer<byte[]> handler) {
            this.inboundHandler = handler;
        }

        @Override
        public void send(MidiMessage message, long timeStamp) {
            byte[] data = message.getMessage();

            System.out.println("RECEIVER: got message " + bytesToHex(data));
            boolean probe = isProbe(data);
            System.out.println("RECEIVER: isProbe=" + probe);

            if (probe) {
                byte[] response = buildResponse(data);
                System.out.println("RECEIVER: sending response " + bytesToHex(response));
                inboundHandler.accept(response);
            }
        }

        @Override public void close() {}

        private boolean isProbe(byte[] msg) {
            // Use mapping’s request format: it will have 3n in parameter_request_format
            return msg.length > 3 &&
                   msg[0] == (byte)0xF0 &&
                   msg[1] == (byte)0x43 &&
                   (msg[2] & 0xF0) == 0x30; // Yamaha request 3n
        }

        private byte[] buildResponse(byte[] probe) {
            // Extract index from the request using mapping
            int index = mapping.extractIndex(probe);

            // For discovery we just return a dummy value (e.g. 1) for that index
            int value = 1;

            // Channel is encoded in the opcode nibble of the request (3n)
            int channel = probe[2] & 0x0F;

            // Build a REAL change message using the mapping (1n, correct length, correct layout)
            return mapping.buildChangeMessage(value, index, channel);
        }
    }

    @Test
    public void FunctionalDiscoveryTest() throws IOException {

        JsonArray desks;
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("MidiControl/discovery/known-desks.json")) {

            desks = new Gson().fromJson(new InputStreamReader(is), JsonArray.class);
        }

        JsonObject deskProfile = desks.get(0).getAsJsonObject();
        SysexMapping mapping = new Gson().fromJson(deskProfile.get("sysexmapping"), SysexMapping.class);
        mapping.initialize();

        String canonicalId = String.format("%s.%s.%d",
            mapping.getControlGroup(), mapping.getSubControl(), 0);

        SimulatedDeskReceiver deskReceiver = new SimulatedDeskReceiver(mapping);

        MockMidiDevice device = new MockMidiDevice(
            new TestMidiDeviceInfo("Mock", "Mock", "Mock", "1.0"),
            1, 1,
            deskReceiver
        );

        OutputRequestSender router = new OutputRequestSender() {
            @Override
            public void send(byte[] msg) {
                try {
                    SysexMessage sysex = new SysexMessage();
                    sysex.setMessage(msg, msg.length);
                    deskReceiver.send(sysex, -1);
                } catch (InvalidMidiDataException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override public void applyRequest(String canonicalId) {}
        };

        ControlInstance ci = new ControlInstance(canonicalId, 0, mapping, null);

        MockCanonicalRegistry registry = new MockCanonicalRegistry();
        registry.registerCanonical(canonicalId, ci);

        deskReceiver.setInboundHandler(bytes -> {
            ControlInstance inst = registry.resolveCanonicalId(canonicalId);
            inst.setLastSysex(bytes);
            inst.updateValue(1);
        });

        ScheduledExecutorService timeoutScheduler = Executors.newScheduledThreadPool(1);
        RehydrationManager rehydrationManager = new RehydrationManager(router, registry, timeoutScheduler);
        rehydrationManager.injectNewRegistry(registry);

        MockMidiIOManager ioManager = new MockMidiIOManager(null);
        ioManager.setRehydrationManager(rehydrationManager);

        MidiDeviceDTO dto = new MidiDeviceDTO();
        dto.canInput = true;
        dto.canOutput = true;
        ioManager.devices.add(dto);

        ioManager.setMidiOutForTest(device);

        DeskDiscovery discoverer = new DeskDiscovery(ioManager);
        DeskDiscoveryResult result = discoverer.discoverDeskModel();
        assertEquals("YAMAHA_01V96I", result.getModel());
    }
}
