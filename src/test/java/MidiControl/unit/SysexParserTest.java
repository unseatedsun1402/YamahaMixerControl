package MidiControl.unit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import MidiControl.SysexUtils.SysexParser;

public class SysexParserTest {
    @org.junit.jupiter.api.Test
    public void testProcessIncomingMidiMessage() {
        byte[] sysexMessage = new byte[] {(byte)0xF0, 0x43, 0x12, 0x00, 0x7F, (byte)0xF7};
        String result=SysexParser.processIncomingMidiMessage(sysexMessage);
        assertEquals("Unknown SysEx message F04312007FF7", result);
    }

    @org.junit.jupiter.api.Test
    public void testProcessIncomingMidiMessage_PhantomPowerOn() {
        // Expanded SysEx message based on mapping
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
}
