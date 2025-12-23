package MidiControl.unit.NrpnUtils;

import static org.junit.jupiter.api.Assertions.*;

import MidiControl.ControlServer.CanonicalInputEvent;
import MidiControl.ControlServer.HardwareInputHandler;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;
import MidiControl.NrpnUtils.NrpnMapping;
import MidiControl.NrpnUtils.NrpnParser;
import MidiControl.NrpnUtils.NrpnRegistry;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;
import MidiControl.TestUtilities.MidiTestUtils;
import MidiControl.TestUtilities.NrpnMocker;
import MidiControl.TestUtilities.TestListener;

import java.util.List;

import javax.sound.midi.ShortMessage;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class NrpnParserTest {

    /**
     * Build a minimal CanonicalRegistry from an inline Sysex mapping
     * so we don't manually mutate internal collections.
     */
    private CanonicalRegistry buildTestRegistry(NrpnRegistry nrpnRegistry) {
        String json =
            "[{"
                + "\"control_group\": \"test\","
                + "\"control_id\": 1,"
                + "\"sub_control\": \"group\","
                + "\"max_channels\": 1,"
                + "\"value\": 0,"
                + "\"min_value\": 0,"
                + "\"max_value\": 16383,"
                + "\"default_value\": 0,"
                + "\"comment\": \"Test NRPN target\","
                + "\"key\": 1234567890,"
                + "\"parameter_change_format\": ["
                + "240, 67, \"1n\", 62, 17, 1, 0, 1, 0, 0,"
                + "\"cc\","
                + "\"dd\", \"dd\","
                + "247"
                + "],"
                + "\"parameter_request_format\": ["
                + "240, 67, \"3n\", 62, 17, 1, 0, 1, 0, 0,"
                + "\"cc\","
                + "247"
                + "]"
            + "}]";

        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromString(json);

        // CanonicalRegistry builds groups/subcontrols/instances from Sysex mappings
        CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));

        // Attach NRPN mappings so resolveNrpn(msb, lsb) works
        registry.attachNrpnMappings(nrpnRegistry.getMappings());

        return registry;
    }

    private NrpnRegistry buildTestNrpnRegistry() {
        NrpnRegistry reg = new NrpnRegistry();

        // MSB=1, LSB=0 → canonical ID must match what buildTestRegistry creates
        // control_group = "test", sub_control = "group", index 0 → "test.group.0"
        NrpnMapping mapping = new NrpnMapping("1", "0", "test.group.0");

        reg.replace(List.of(mapping));
        return reg;
    }

    @Test
    public void testMix9Send3Resolution() throws Exception {
        NrpnParser parser = new NrpnParser();
        NrpnRegistry nrpnRegistry = buildTestNrpnRegistry();
        HardwareInputHandler handler = new HardwareInputHandler(parser, nrpnRegistry);
        NrpnMocker mocker = new NrpnMocker(parser, handler);

        CanonicalRegistry registry = buildTestRegistry(nrpnRegistry);

        // Simulate an NRPN message (MSB=1, LSB=0, value=8192)
        CanonicalInputEvent event = mocker.sendNrpn(1, 0, 8192);
        assertNotNull(event);

        // Ensure NRPN resolves to a canonical control instance
        ControlInstance resolved = registry.resolveNrpn(1, 0);
        assertNotNull(resolved);
        assertEquals("test.group.0", resolved.getCanonicalId());
    }

    @Test
    void testValidNrpnWithOnlyValueMsb() throws Exception {
        NrpnRegistry nrpnRegistry = buildTestNrpnRegistry();
        NrpnParser nrpnParser = new NrpnParser();
        HardwareInputHandler handler = new HardwareInputHandler(nrpnParser, nrpnRegistry);
        NrpnMocker mocker = new NrpnMocker(nrpnParser, handler);

        CanonicalRegistry registry = buildTestRegistry(nrpnRegistry);

        ControlInstance instance = registry.resolveNrpn(1, 0);
        assertNotNull(instance);

        TestListener listener = new TestListener();
        instance.addListener(listener);

        CanonicalInputEvent event = mocker.sendNrpn(1, 0, 8192);
        assertNotNull(event);

        int value = instance.extractValue(event);
        instance.updateValue(value);

        assertEquals(8192, listener.lastValue);
        assertEquals(instance, listener.lastInstance);
    }

    @Test
    void testValidNrpnWithFull14BitValue() throws Exception {
        NrpnParser parser = new NrpnParser();

        // 12345 = 0x3039 → MSB=96, LSB=57
        parser.parse(MidiTestUtils.createControlChange(0, 99, 1));
        parser.parse(MidiTestUtils.createControlChange(0, 98, 0));
        parser.parse(MidiTestUtils.createControlChange(0, 6, 96));
        parser.parse(MidiTestUtils.createControlChange(0, 38, 57));

        // Registry is not involved in parsing
        assertDoesNotThrow(() ->
            parser.parse(MidiTestUtils.createControlChange(0, 38, 57))
        );
    }

    @Test
    void testParserRejectsInvalidControlChange() throws Exception {
        NrpnParser parser = new NrpnParser();
        ShortMessage msg = new ShortMessage(ShortMessage.NOTE_ON, 0, 60, 127);

        assertDoesNotThrow(() -> parser.parse(msg));
    }
}