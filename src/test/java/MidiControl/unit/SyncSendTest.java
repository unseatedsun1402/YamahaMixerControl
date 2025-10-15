package MidiControl.unit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import MidiControl.SyncSend;
import MidiControl.Mocks.MockMidiOutput;

import javax.sound.midi.ShortMessage;

import java.util.logging.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import static org.junit.jupiter.api.Assertions.*;

@Tag("Unit")
public class SyncSendTest {

    @Test
    public void testSyncSendDispatchesMidiMessages() throws Exception {
        // Arrange
        ConcurrentLinkedQueue<ShortMessage[]> buffer = new ConcurrentLinkedQueue<>();
        MockMidiOutput mockOutput = new MockMidiOutput();

        ShortMessage msg1 = new ShortMessage();
        msg1.setMessage(ShortMessage.CONTROL_CHANGE, 0, 7, 100);
        ShortMessage msg2 = new ShortMessage();
        msg2.setMessage(ShortMessage.NOTE_ON, 0, 60, 127);

        buffer.add(new ShortMessage[]{msg1, msg2});

        SyncSend syncSend = new SyncSend(buffer, mockOutput);
        Thread thread = new Thread(syncSend);

        // Act
        thread.start();

        // Wait for processing
        int attempts = 0;
        while (mockOutput.getSentMessages().size() < 2 && attempts < 10) {
            Thread.sleep(10);
            attempts++;
        }

        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(SyncSend.class.getName()).log(Level.FINE, "SyncSend interrupted during join", ex);
            Thread.currentThread().interrupt();
        }

        // Assert
        assertEquals(2, mockOutput.getSentMessages().size(), "Expected 2 MIDI messages to be sent");
        assertEquals(ShortMessage.CONTROL_CHANGE, mockOutput.getSentMessages().get(0).getCommand());
        assertEquals(ShortMessage.NOTE_ON, mockOutput.getSentMessages().get(1).getCommand());
    }
}


