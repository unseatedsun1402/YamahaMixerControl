package MidiControl.MidiDeviceManager;

import MidiControl.SysexUtils.SysexParser;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;

public class MidiInputReceiver implements Receiver {

    private final ConcurrentLinkedQueue<MidiMessage> inputBuffer;
    private volatile MidiIngressListener ingressListener = bytes -> {};

    private static boolean DEBUG = false;
    private volatile boolean open = true;

    private static final Logger logger =
            Logger.getLogger(MidiInputReceiver.class.getName());

    public MidiInputReceiver(ConcurrentLinkedQueue<MidiMessage> inputBuffer) {
        this.inputBuffer = inputBuffer;
    }

    public void setIngressListener(MidiIngressListener listener) {
        if (listener != null) {
            this.ingressListener = listener;
        }
    }

    public static void enableDebug() {
        DEBUG = true;
    }

    @Override
    public void close() {
        open = false;
    }

    @Override
    public void send(MidiMessage message, long timeStamp) {
        if (message == null) {
            logger.warning("MidiMessage is null");
            return;
        }

        if (!open) {
            logger.warning("Midi device is not open: " + this.hashCode());
            return;
        }

        byte[] bytes = message.getMessage();
        ingressListener.onBytesReceived(bytes.length);
        inputBuffer.add(message);

        if (DEBUG) {
            logger.info("Added to inputBuffer: " +
                SysexParser.bytesToHex(bytes));
        }
    }

    public Boolean isOpen() {
        return open;
    }
}