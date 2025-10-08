package MidiControl;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Receives incoming MIDI messages and puts them into a buffer for processing.
 */
public class MidiInputReceiver implements Receiver {
    private final ConcurrentLinkedQueue<MidiMessage> inputBuffer;
    private volatile boolean open = true;

    public MidiInputReceiver(ConcurrentLinkedQueue<MidiMessage> inputBuffer) {
        this.inputBuffer = inputBuffer;
    }

    @Override
public void send(MidiMessage message, long timeStamp) {
    if (open && message != null) {
        inputBuffer.add(message);
    }
}

    @Override
    public void close() {
        open = false;
    }
}
