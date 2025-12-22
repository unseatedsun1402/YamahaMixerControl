package MidiControl.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import MidiControl.MidiServer;
import MidiControl.NrpnUtils.NrpnParser;

public class BroadcastBufferTest {
    @Test
    public void testBufferMidiDoesNotThrow() {
        MidiServer server = new MidiServer();
        NrpnParser.setDispatchTarget(server);

        String json = "{\"id\":\"test\",\"msb\":1,\"lsb\":2,\"value\":100}";

        assertDoesNotThrow(() -> MidiServer.bufferMidi(json));
        assertTrue(MidiServer.getOutputBufferSize() > 0, "Buffer size should be greater than 0 after buffering MIDI");
    }
}
