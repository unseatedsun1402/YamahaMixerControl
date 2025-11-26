package MidiControl.unit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.sound.midi.MidiMessage;

import MidiControl.MidiInputReceiver;

@Tag("unit")
public class MidiInputReceiverTest {
    
    @org.junit.jupiter.api.Test
    void testReceiverHandlesNullMessageGracefully() {
        ConcurrentLinkedQueue<MidiMessage> buffer = new ConcurrentLinkedQueue<>();
        MidiInputReceiver receiver = new MidiInputReceiver(buffer);
        assertDoesNotThrow(() -> receiver.send(null, -1), "Receiver should not throw on null message");
        assertTrue(buffer.isEmpty(), "Buffer should remain empty on null input");
        receiver.close();
    }
}
