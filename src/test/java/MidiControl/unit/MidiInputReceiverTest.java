package MidiControl.unit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import MidiControl.MidiInputReceiver;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.sound.midi.MidiMessage;
import org.junit.jupiter.api.Tag;

@Tag("unit")
public class MidiInputReceiverTest {

  @org.junit.jupiter.api.Test
  void testReceiverHandlesNullMessageGracefully() {
    ConcurrentLinkedQueue<MidiMessage> buffer = new ConcurrentLinkedQueue<>();
    MidiInputReceiver receiver = new MidiInputReceiver(buffer);
    receiver.send(null, -1);
    // assertDoesNotThrow(() -> receiver.send(null, -1), "Receiver should not throw on null
    // message");
    assertTrue(buffer.isEmpty(), "Buffer should remain empty on null input");
    receiver.close();
  }
}
