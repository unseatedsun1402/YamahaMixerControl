package MidiControl.MidiDeviceManager;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReceiverWrapper implements MidiOutput {
    private final MidiDevice device;
    private Receiver receiver;
    private static final Logger logger = Logger.getLogger(ReceiverWrapper.class.getName());

    public ReceiverWrapper(MidiDevice device) throws MidiUnavailableException {
        this.device = device;
        setup();
    }

    private void setup() throws MidiUnavailableException {
        if (!device.isOpen()) {
            device.open();
            logger.log(Level.INFO, "Opened MIDI device for receiver.");
        }

        receiver = device.getReceiver();
        logger.log(Level.INFO, "Receiver initialized and ready.");
    }

    @Override
    public void sendMessage(ShortMessage message) {
        if (receiver != null) {
            receiver.send(message, -1); // -1 = immediate
            logger.log(Level.FINE, "Sent MIDI: cmd={0}, data1={1}, data2={2}",
                new Object[]{message.getCommand(), message.getData1(), message.getData2()});
        } else {
            logger.log(Level.WARNING, "Receiver is null. Cannot send MIDI message.");
        }
    }

    public Receiver getRawReceiver() {
        return receiver;
    }

    public void close() {
        if (receiver != null) {
            receiver.close();
        }
        if (device.isOpen()) {
            device.close();
        }
        logger.log(Level.INFO, "Closed receiver and MIDI device.");
    }

    public boolean isOpen(){
        return device.isOpen();
    }

    @Override
    public MidiDevice.Info getDeviceInfo() {
        return this.device.getDeviceInfo();
    }
}