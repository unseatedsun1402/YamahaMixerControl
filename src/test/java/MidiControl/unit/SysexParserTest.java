package MidiControl.unit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;
import MidiControl.SysexUtils.SysexRegistry;

public class SysexParserTest {

    @org.junit.jupiter.api.Test
    public void testProcessIncomingMidiMessage_PhantomPowerOn() {
        // Expanded Sysex message based on mapping
        byte[] phantomOn = new byte[] {
        (byte)0xF0, (byte)0x43, (byte)0x10, (byte)0x3E,
        (byte)0x11, (byte)0x01, (byte)0x00, (byte)0x29,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, // pos 4 -> channel index
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x01, (byte)0xF7
        };

        String result = SysexParser.processIncomingMidiMessage(phantomOn);
        Logger.getLogger(SysexParserTest.class.getName()).info("Result: " + result);

        // Assert that the parser resolves correctly
        assertEquals("kInputHA (kHAPhantom) = On", result);
    }
    @Test
    void testResolveValidMessageByStringJson() {
        // Minimal JSON with one mapping
        String json = "[{"
        + "\"control_group\": \"kInputHA\","
        + "\"control_id\": 41,"
        + "\"sub_control\": \"kHAPhantom\","
        + "\"channel_index\": 1,"
        + "\"value\": 0,"
        + "\"min_value\": 0,"
        + "\"max_value\": 1,"
        + "\"default_value\": 0,"
        + "\"comment\": \"Off, On\","
        + "\"parameter_change_format\": [240,67,\"1n\",62,17,1,0,41,0,0,0,14,\"dd\",\"dd\",\"dd\",\"dd\",\"dd\",247],"
        + "\"parameter_request_format\": [240,67,\"3n\",62,17,1,0,41,0,0,0,14,247]"
        + "}]";

        // Load mapping from JSON
        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromString(json);
        SysexRegistry registry = new SysexRegistry(mappings);

        // Build a message that matches the mapping
        byte[] message = new byte[] {
        (byte)0xF0, (byte)0x43, (byte)0x10, (byte)0x3E,
        (byte)0x11, (byte)0x01, (byte)0x00, (byte)0x29,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, // pos 4 -> channel index
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x01, (byte)0xF7
        };

        SysexMapping result = registry.resolve(message);

        assertNotNull(result, "Expected mapping to be found");
        assertEquals("kInputHA", result.getControlGroup());
    }

    @Test
    void testResolveNoMatch() {
        SysexRegistry registry = new SysexRegistry(List.of());
        byte[] message = new byte[18];
        SysexMapping result = registry.resolve(message);
        assertNull(result, "Expected no mapping to be found");
    }

}
