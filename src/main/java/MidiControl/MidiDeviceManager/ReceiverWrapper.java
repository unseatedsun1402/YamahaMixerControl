package MidiControl.MidiDeviceManager;

import MidiControl.ControlServer.HardwareInputHandler;
import MidiControl.SysexUtils.SysexParser;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;

public class ReceiverWrapper implements MidiOutput {

    private final MidiDevice device;
    private Receiver receiver;

    private static final Logger logger =
            Logger.getLogger(HardwareInputHandler.class.getName());

    private static boolean DEBUG = false;

    public static void enableDebug() {
        DEBUG = true;
        logger.info("ReceiverWrapper debug enabled");
    }

    // ------------------------------------------------------------

    public ReceiverWrapper(MidiDevice device) throws MidiUnavailableException {
        this.device = device;
        setup();
    }

    private void setup() throws MidiUnavailableException {
        if (!device.isOpen()) {
            device.open();
            logger.log(Level.INFO,
                "Opened MIDI device for receiver: " + device.getDeviceInfo().getName());
        }

        if (device.getMaxReceivers() != 0) {
            logger.info("Max receivers: " + device.getMaxReceivers());
            receiver = device.getReceiver();
        } else {
            throw new MidiUnavailableException("Device has no receiver available.");
        }

        logger.log(Level.INFO, "Receiver initialized and ready.");
    }

    @Override
    public void sendMessage(byte[] data) {
        if (receiver == null) {
            logger.warning("Receiver is null. Cannot send MIDI message.");
            return;
        }

        try {
            // ------------------------------------------------------------
            // SysEx
            // ------------------------------------------------------------
            if ((data[0] & 0xFF) == 0xF0) {
                if (DEBUG) {
                    logger.info("ReceiverWrapper SEND SysEx: " + SysexParser.bytesToHex(data));
                }

                SysexMessage sysex = new SysexMessage();
                sysex.setMessage(data, data.length);
                receiver.send(sysex, -1);

                if (!DEBUG) {
                    logger.info("Sent SysEx: " + SysexParser.bytesToHex(data));
                }
                return;
            }

            // ------------------------------------------------------------
            // ShortMessage (CC, NRPN, Note, etc.)
            // ------------------------------------------------------------
            if (data.length >= 3) {
                if (DEBUG) {
                    logger.info(String.format(
                        "ReceiverWrapper SEND ShortMessage: cmd=%02X data1=%02X data2=%02X",
                        data[0] & 0xFF, data[1] & 0xFF, data[2] & 0xFF
                    ));
                }

                ShortMessage sm = new ShortMessage();
                sm.setMessage(
                    data[0] & 0xFF,
                    data[1] & 0xFF,
                    data[2] & 0xFF
                );
                receiver.send(sm, -1);

                if (!DEBUG) {
                    logger.fine(String.format(
                        "Sent ShortMessage: cmd=%02X data1=%02X data2=%02X",
                        data[0] & 0xFF, data[1] & 0xFF, data[2] & 0xFF
                    ));
                }
                return;
            }

            logger.warning("Invalid MIDI byte[] length: " + data.length);

        } catch (Exception ex) {
            logger.warning("Failed to send MIDI: " + ex.getMessage());
        }
    }

    @Override
    public void sendMessage(MidiMessage message) {
        if (receiver != null) {

            if (DEBUG) {
                logger.info("ReceiverWrapper SEND (legacy): "
                        + SysexParser.bytesToHex(message.getMessage()));
            }

            receiver.send(message, -1);

            if (!DEBUG) {
                logger.fine("Sent MIDI (legacy): "
                        + SysexParser.bytesToHex(message.getMessage()));
            }

        } else {
            logger.warning("Receiver is null. Cannot send MIDI message.");
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

    public boolean isOpen() {
        return device.isOpen();
    }

    @Override
    public MidiDevice.Info getDeviceInfo() {
        return this.device.getDeviceInfo();
    }
}