package MidiControl.functional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.sound.midi.SysexMessage;

import MidiControl.MidiServer;

public class MidiServerSysexTest {
    @org.junit.jupiter.api.Test
    void testSysExReception() throws Exception {
        // Example SysEx message: F0 43 10 4C 00 00 7F F7 (Yamaha style)
        byte[] sysexData = new byte[] {(byte)0xF0, 0x43, 0x10, 0x4C, 0x00, 0x00, 0x7F, (byte)0xF7};
        SysexMessage sysex = new SysexMessage();
        sysex.setMessage(sysexData, sysexData.length);

        MidiServer.addtoinputqueue(sysex);
        MidiServer server = new MidiServer();
        assertDoesNotThrow(server::processIncomingMidi, "SysEx parsing should not throw exceptions");
    }
}
