package MidiControl.MidiDeviceManager;

import javax.sound.midi.*;
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
        if (device == null) {
            logger.log(Level.SEVERE, "MIDI device is null — cannot initialize receiver.");
            return;
        }

        if (!device.isOpen()) {
            device.open();
            logger.log(Level.INFO, "Opened MIDI device for receiver.");
        }

        try {
            receiver = device.getReceiver();
            logger.log(Level.INFO, "Receiver initialized and ready.");
        } catch (MidiUnavailableException e) {
            logger.log(Level.SEVERE, "Failed to get receiver from device", e);
            receiver = null;
        }
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
        if (device != null && device.isOpen()) {
            device.close();
        }
        logger.log(Level.INFO, "Closed receiver and MIDI device.");
    }

    public boolean isOpen() {
        return device != null && device.isOpen();
    }

    public boolean isAvailable() {
        return device != null && receiver != null;
    }

    @Override
    public MidiDevice.Info getDeviceInfo() {
        if (device == null) {
            logger.log(Level.WARNING, "Device is null — cannot get device info.");
            return null;
        }
        return device.getDeviceInfo();
    }

    public String getDeviceName() {
        MidiDevice.Info info = getDeviceInfo();
        return info != null ? info.getName() : "Unknown";
    }

    public String getDeviceDescription() {
        MidiDevice.Info info = getDeviceInfo();
        return info != null ? info.getDescription() : "Unknown";
    }

    public int getDeviceIndex() {
        MidiDevice.Info info = getDeviceInfo();
        return info != null ? MidiDeviceUtils.getDeviceIndex(info) : -1;
    }
}