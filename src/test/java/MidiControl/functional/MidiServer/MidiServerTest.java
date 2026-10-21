package MidiControl.functional.MidiServer;

import static org.junit.jupiter.api.Assertions.*;

import MidiControl.MidiDeviceManager.MidiInputReceiver;
import MidiControl.Mocks.MockMidiOut;
import MidiControl.Server.MidiServer;
import MidiControl.TestUtilities.MidiTestUtils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Tag("functional")
public class MidiServerTest {
    private final List<MidiServer> servers = new ArrayList<>();

    private MidiServer createServer() {
        MidiServer server = new MidiServer();
        servers.add(server);
        return server;
    }

    @AfterEach
    void shutdownServers() {
        servers.forEach(MidiServer::shutdown);
        servers.clear();
    }


    @Test
    void testInputReceiverReceivesMessage() throws Exception {
        ConcurrentLinkedQueue<MidiMessage> inputBuffer = new ConcurrentLinkedQueue<>();

        try (MidiInputReceiver receiver = new MidiInputReceiver(inputBuffer)) {
            ShortMessage msg = new ShortMessage();
            msg.setMessage(ShortMessage.NOTE_ON, 0, 60, 127);

            receiver.send(msg, -1);

            assertFalse(inputBuffer.isEmpty(), "Input buffer should not be empty after receiving a message");
            assertEquals(1, inputBuffer.size(), "Input buffer should contain exactly one message");
            assertEquals(msg, inputBuffer.poll(), "Received message should match sent message");
        }
    }

    @Test
    void testSendMidiMessagesDispatchesToOutput() {
        MidiServer server = createServer();

        MockMidiOut mockOutput = new MockMidiOut();
        server.getMidiDeviceManager().setMidiOutForTest(mockOutput);

        byte[] msg1 = new byte[] {(byte) 0xB0, 7, 100};   // CC7
        byte[] msg2 = new byte[] {(byte) 0x90, 60, 127};  // Note On

        server.getMidiDeviceManager().getMidiOut().sendMessage(msg1);
        server.getMidiDeviceManager().getMidiOut().sendMessage(msg2);

        List<byte[]> sent = mockOutput.getSentMessages();

        assertEquals(2, sent.size(), "Expected 2 MIDI messages to be sent");
        assertArrayEquals(msg1, sent.get(0));
        assertArrayEquals(msg2, sent.get(1));
    }

    @Test
    void testIncomingMidiParsingDoesNotThrow() throws InvalidMidiDataException {
        ShortMessage msg = new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 99, 1);
        MidiServer server = createServer();
        server.addtoinputqueue(msg);
        assertDoesNotThrow(server::run, "Processing incoming MIDI should not throw exceptions");
    }

    @Test
    void testSetInputDeviceWithOutputOnlyIndexFailsGracefully() throws Exception {
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        assertTrue(infos.length > 0, "Expected at least one MIDI device for this test");

        MidiDevice device = MidiSystem.getMidiDevice(infos[0]);

        if (device.getMaxTransmitters() == 0) {
            MidiServer server = createServer();
            boolean wasOpen = device.isOpen();

            assertDoesNotThrow(
                () -> server.getMidiDeviceManager().trySetInputDevice(0),
            "Should throw for invalid device index");
            if (!wasOpen) {
                assertFalse(device.isOpen(), "An output-only device must be closed after input setup fails");
            }
        }
    }

    @Test
    void testProcessIncomingMidiHandlesEmptyBuffer() {
        MidiServer server = createServer();
        assertDoesNotThrow(server::run, "Processing with empty buffer should not throw");
    }

    @Test
    void testInputQueueHelperAddsMessages() throws Exception {
        ShortMessage msg = MidiTestUtils.createControlChange(0, 99, 1);
        MidiServer server = createServer();
        int before = server.getInputBufferSize();
        server.addtoinputqueue(msg);
        int after = server.getInputBufferSize();
        assertEquals(before + 1, after, "Input buffer should grow by 1 after adding a message");
    }
}
