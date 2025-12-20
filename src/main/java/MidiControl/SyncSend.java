package MidiControl;

import javax.sound.midi.MidiMessage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SyncSend implements Runnable {
    private final ConcurrentLinkedQueue<MidiMessage[]> buffer;

    public SyncSend(ConcurrentLinkedQueue<MidiMessage[]> buffer) {
        this.buffer = buffer;
    }

    @Override
    public void run() {
        Logger.getLogger(SyncSend.class.getName()).log(Level.INFO, "SyncSend thread started.", (Object) null);

        while (!Thread.currentThread().isInterrupted()) {
            MidiMessage[] commands = buffer.poll();

            if (commands != null && MidiServer.midiOut != null) {
                Logger.getLogger(SyncSend.class.getName()).log(Level.FINE, "Buffer populated", (Object) null);
                // Logger.getLogger(SyncSend.class.getName()).log(Level.INFO, "Processing the buffer and sending to the midi output device", (Object) null);

                for (MidiMessage databyte : commands) {
                    try {
                        MidiServer.midiOut.sendMessage(databyte);
                        Logger.getLogger(SyncSend.class.getName()).log(Level.FINE,
                            "Server sending midi event: " + MidiServer.midiOut.getClass().getSimpleName() + " " +
                            databyte.getMessage()[0] + " " + databyte.getMessage()[1] + " " + databyte.getMessage()[2],
                            (Object) null);
                        Thread.sleep(2);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(SyncSend.class.getName()).log(Level.FINE, "SyncSend interrupted during send", ex);
                        Thread.currentThread().interrupt(); // Preserve interrupt status
                    }
                }
            } else {
                try {
                    Thread.sleep(5); // Yield to avoid busy loop
                } catch (InterruptedException ex) {
                    Logger.getLogger(SyncSend.class.getName()).log(Level.FINE, "SyncSend interrupted during idle", ex);
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
