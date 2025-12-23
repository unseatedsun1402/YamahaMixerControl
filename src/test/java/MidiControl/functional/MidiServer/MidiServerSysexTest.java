package MidiControl.functional.MidiServer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import javax.sound.midi.SysexMessage;

import MidiControl.Server.MidiServer;

public class MidiServerSysexTest {
  @org.junit.jupiter.api.Test
  public void testSysExReception() throws Exception {
    // Example SysEx message: F0 43 10 4C 00 00 7F F7 (Yamaha style)
    MidiServer server = new MidiServer();
    byte[] sysexData = new byte[] {(byte) 0xF0, 0x43, 0x10, 0x4C, 0x00, 0x00, 0x7F, (byte) 0xF7};
    SysexMessage sysex = new SysexMessage();
    sysex.setMessage(sysexData, sysexData.length);
    assertArrayEquals(sysexData, sysex.getMessage());
    
    server.addtoinputqueue(sysex);
    // assertDoesNotThrow(() -> server.run(), "SysEx parsing should not throw exceptions");
  }
}
