package MidiControl;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

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
        // Logger.getLogger(getClass().getName()).info("Received MIDI message: " + Arrays.toString(message.getMessage()));
        inputBuffer.add(message);
        // Logger.getLogger(getClass().getName()).info("Added to inputBuffer: " + Arrays.toString(message.getMessage()));
        // Logger.getLogger(getClass().getName()).info("inputBuffer size after add: " + inputBuffer.size());
    }
}

    @Override
    public void close() {
        open = false;
    }
}
