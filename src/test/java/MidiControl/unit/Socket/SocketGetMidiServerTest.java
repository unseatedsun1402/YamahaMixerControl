package MidiControl.unit.Socket;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import MidiControl.MidiServer;
import MidiControl.Socket;

public class SocketGetMidiServerTest {
    @Test
    public void testGetMidiServer() {
        assertDoesNotThrow(() -> new Socket().getServer());
    }

    @Test
    public void testInitWithoutMidiAvailable() {
        assertDoesNotThrow(() -> {
            MidiServer server = new Socket().getServer();
            assertTrue(MidiServer.getReceiver() == null);
            assertTrue(MidiServer.getTransmitter() == null);
            assertDoesNotThrow(() -> server.shutdown());
        });
    }

}
