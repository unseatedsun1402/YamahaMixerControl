package MidiControl;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import javax.sound.midi.MidiMessage;

import MidiControl.MidiDeviceManager.MidiIOManager;
import MidiControl.MidiDeviceManager.MidiOutput;

public class SyncSend implements Runnable {

    private final ConcurrentLinkedQueue<MidiMessage[]> buffer;
    private final MidiIOManager ioManager;
    private static final Logger logger = Logger.getLogger(SyncSend.class.getName());

    public SyncSend(ConcurrentLinkedQueue<MidiMessage[]> buffer, MidiIOManager ioManager) {
        this.buffer = buffer;
        this.ioManager = ioManager;
    }

    @Override
    public void run() {
        logger.info("SyncSend thread started");

        while (!Thread.currentThread().isInterrupted()) {

            MidiMessage[] commands = buffer.poll();
            MidiOutput midiOut = ioManager.getMidiOut();
            if (commands != null && midiOut != null) {

                for (MidiMessage msg : commands) {
                    try {
                        logger.fine("Sending msg: " + msg);
                        midiOut.sendMessage(msg);
                        Thread.sleep(2);

                    } catch (InterruptedException e) {
                        logger.fine("Interrupted during send: " + e);
                        Thread.currentThread().interrupt();
                    }
                }

            } else {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}