package MidiControl.MidiDeviceManager;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import java.util.logging.Logger;

public class ReceiverWrapper implements MidiOutput {
    private final Receiver receiver;
    private static final Logger logger = Logger.getLogger(ReceiverWrapper.class.getName());

    public ReceiverWrapper(MidiDevice device) throws MidiUnavailableException {
        this.receiver = device.getReceiver();
    }

    @Override
    public void sendMessage(ShortMessage message) {
        receiver.send(message, -1); // -1 = immediate
        logger.info("Sent MIDI: cmd=" + message.getCommand() +
                    ", data1=" + message.getData1() +
                    ", data2=" + message.getData2());
    }

    public Receiver getRawReceiver() {
        return receiver;
    }
}
