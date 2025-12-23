package MidiControl.functional.CanonicalRegistry;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import MidiControl.ControlServer.GuiInputHandler;
import MidiControl.ControlServer.OutputRouter;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;
import MidiControl.MidiDeviceManager.MidiIOManager;
import MidiControl.MidiDeviceManager.ReceiverWrapper;
import MidiControl.Mocks.MockMidiOut;
import MidiControl.NrpnUtils.NrpnMapping;
import MidiControl.NrpnUtils.NrpnMappingLoader;
import MidiControl.Server.MidiServer;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;

public class ServerToHardwareTest {
    @Test
    public void testResolveAndBuildForFader1() throws Exception {

        // 1. Load real mappings (whatever your real loader is)
        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");
        List<NrpnMapping> nrpnMappings = NrpnMappingLoader.loadFromResource("MidiControl/nrpn/01v96i_nrpn_mappings.json");
        CanonicalRegistry registry = new CanonicalRegistry(mappings,new SysexParser(mappings));

        // 2. Resolve the canonical ID exactly as the GUI sends it
        String canonicalId = "kInputFader.kFader.1";
        ControlInstance instance = registry.resolve(canonicalId);

        assertNotNull(instance, "Registry should resolve canonical ID: " + canonicalId);

        // 3. Get the mapping attached to this instance
        SysexMapping mapping = instance.getSysex();
        assertNotNull(mapping, "Instance should have a SysexMapping attached");

        // 4. Build SysEx for a known test value
        int testValue = 770; // matches your earlier example
        byte[] built = mapping.buildChangeMessage(testValue, instance.getIndex());

        assertNotNull(built, "Builder should return a SysEx byte array");

        // 5. Build expected bytes from the mapping format
        // byte[] expected = mapping.buildExpectedBytes(testValue, instance.getIndex());
        byte[] expected = {(byte)0xf0,0x43,0x10,0x3e,0x7f,0x01,0x1C,0x00,0x01,0x00,0x00,0x06,0x02,(byte) 0xf7};

        System.out.println("Built sysex " + bytesToHex(built));
        System.out.println("Expct sysex " + bytesToHex(expected));

        // 6. Compare actual vs expected
        assertArrayEquals(expected, built,
            "Built SysEx must match mapping-defined SysEx format");
    }

    public static String bytesToHex(byte[] message) {
    StringBuilder sb = new StringBuilder();
    for (byte b : message) {
      sb.append(String.format("%02X", b));
    }
    return sb.toString();
  }

    @Test
    public void testResolveBuildAndRouteForFader1() throws Exception {

        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");
        CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));
        MidiServer server = new MidiServer(registry);

        String canonicalId = "kInputFader.kFader.1";
        ControlInstance instance = registry.resolve(canonicalId);
        assertNotNull(instance);

        SysexMapping mapping = instance.getSysex();
        assertNotNull(mapping);

        int testValue = 770;
        byte[] built = mapping.buildChangeMessage(testValue, instance.getIndex());

        // Route through the OutputRouter
        OutputRouter router = new OutputRouter(server);
        MidiIOManager manager = server.getMidiDeviceManager();
        MockMidiOut out = new MockMidiOut();
        manager.setMidiOutForTest(out);
        ReceiverWrapper.enableDebug();

        router.applyChange(canonicalId, testValue);

        System.out.println("Built sysex  " + bytesToHex(built));

        byte[] expected = {(byte)0xf0,0x43,0x10,0x3e,0x7f,0x01,0x1C,0x00,0x01,0x00,0x00,0x06,0x02,(byte)0xf7};

        assertArrayEquals(expected, out.sent.get(0),
            "OutputRouter must not corrupt SysEx between resolve and MIDI output");
    }


        @Test
        public void testResolveBuildAndGuiHandleForFader1() throws Exception {

            List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");
            CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));
            MidiServer server = new MidiServer(registry);

            String canonicalId = "kInputFader.kFader.1";
            ControlInstance instance = registry.resolve(canonicalId);
            assertNotNull(instance);

            SysexMapping mapping = instance.getSysex();
            assertNotNull(mapping);

            int testValue = 770;
            byte[] built = mapping.buildChangeMessage(testValue, instance.getIndex());

            // Route through the OutputRouter
            MidiIOManager manager = server.getMidiDeviceManager();
            MockMidiOut out = new MockMidiOut();
            manager.setMidiOutForTest(out);
            ReceiverWrapper.enableDebug();

            OutputRouter router = new OutputRouter(server);
            GuiInputHandler handler = new GuiInputHandler(router);
            
            handler.handleGuiChange(canonicalId, testValue);

            System.out.println("Built sysex  " + bytesToHex(built));

            byte[] expected = {(byte)0xf0,0x43,0x10,0x3e,0x7f,0x01,0x1C,0x00,0x01,0x00,0x00,0x06,0x02,(byte)0xf7};

            assertArrayEquals(expected, out.sent.get(0),
                "OutputRouter must not corrupt SysEx between resolve and MIDI output");
        }
}
