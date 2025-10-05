package MidiControl;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.MidiMessage;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MidiServerTest {
    @Test
    void testAddToSendQueue() {
        ShortMessage[] commands = new ShortMessage[1];
        try {
            commands[0] = new ShortMessage();
            commands[0].setMessage(ShortMessage.NOTE_ON, 0, 60, 127);
        } catch (Exception e) {
            org.junit.jupiter.api.Assertions.fail("Failed to create ShortMessage");
        }
        int before = MidiServer.buffer.size();
        MidiServer.addtosendqueue(commands);
        int after = MidiServer.buffer.size();
        org.junit.jupiter.api.Assertions.assertEquals(before + 1, after, "Buffer should have one more element after adding");
    }

    @Test
    void testInputReceiverReceivesMessage() {
        ConcurrentLinkedQueue<MidiMessage> inputBuffer = new ConcurrentLinkedQueue<>();
        try (MidiInputReceiver receiver = new MidiInputReceiver(inputBuffer)) {
            ShortMessage msg = new ShortMessage();
            try {
                msg.setMessage(ShortMessage.NOTE_ON, 0, 60, 127);
            } catch (Exception e) {
                org.junit.jupiter.api.Assertions.fail("Failed to create ShortMessage");
            }
            receiver.send(msg, -1);
            org.junit.jupiter.api.Assertions.assertFalse(inputBuffer.isEmpty(), "Input buffer should not be empty after receiving a message");
            org.junit.jupiter.api.Assertions.assertEquals(msg, inputBuffer.poll(), "Received message should match sent message");
        }
    }
}
