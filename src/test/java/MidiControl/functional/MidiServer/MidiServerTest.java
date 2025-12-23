package MidiControl.functional.MidiServer;

import static org.junit.jupiter.api.Assertions.*;

import MidiControl.MidiInputReceiver;
import MidiControl.Mocks.MockMidiOut;
import MidiControl.Server.MidiServer;
import MidiControl.TestUtilities.MidiTestUtils;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Tag("functional")
public class MidiServerTest {

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
        MidiServer server = new MidiServer();

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
        MidiServer server = new MidiServer();
        server.addtoinputqueue(msg);
        assertDoesNotThrow(server::run, "Processing incoming MIDI should not throw exceptions");
    }

    @Test
    void testSetInputDeviceWithInvalidIndexThrows() {
        MidiServer server = new MidiServer();

        assertThrows(
            MidiUnavailableException.class,
            () -> server.getMidiDeviceManager().setInputDevice(999),
            "Should throw for invalid device index");
    }

    @Test
    void testSetOutputDeviceWithInvalidIndexThrows() {
        MidiServer server = new MidiServer();

        assertThrows(
            MidiUnavailableException.class,
            () -> server.getMidiDeviceManager().setOutputDevice(999),
            "Should throw for invalid device index");
    }

    @Test
    void testSetInputDeviceWithOutputOnlyIndexFailsGracefully() throws Exception {
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        assertTrue(infos.length > 0, "Expected at least one MIDI device for this test");

        MidiDevice device = MidiSystem.getMidiDevice(infos[0]);

        if (device.getMaxTransmitters() == 0) {
            MidiServer server = new MidiServer();

            assertDoesNotThrow(
                () -> {
                    try {
                        server.getMidiDeviceManager().setInputDevice(0);
                    } catch (MidiUnavailableException e) {
                        // Device is valid but not usable as input; this is acceptable for this test.
                    }
                },
                "Should not crash when setting output-only device as input");
        }
    }

    @Test
    void testProcessIncomingMidiHandlesEmptyBuffer() {
        MidiServer server = new MidiServer();
        assertDoesNotThrow(server::run, "Processing with empty buffer should not throw");
    }

    @Test
    void testInputQueueHelperAddsMessages() throws Exception {
        ShortMessage msg = MidiTestUtils.createControlChange(0, 99, 1);
        MidiServer server = new MidiServer();
        int before = server.getInputBufferSize();
        server.addtoinputqueue(msg);
        int after = server.getInputBufferSize();
        assertEquals(before + 1, after, "Input buffer should grow by 1 after adding a message");
    }
}