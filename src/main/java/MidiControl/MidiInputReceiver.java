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
        Logger.getLogger(getClass().getName()).fine("Received MIDI message to send: " + Arrays.toString(message.getMessage()));
        inputBuffer.add(message);
    }
}

    @Override
    public void close() {
        open = false;
    }
}
