package MidiControl;

import javax.sound.midi.ShortMessage;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SyncSend implements Runnable {
    private final ConcurrentLinkedQueue<ShortMessage[]> buffer;

    public SyncSend(ConcurrentLinkedQueue<ShortMessage[]> buffer) {
        this.buffer = buffer;
    }

    @Override
    public void run() {
        Logger.getLogger(SyncSend.class.getName()).log(Level.INFO, "SyncSend thread started.", (Object) null);

        while (!Thread.currentThread().isInterrupted()) {
            ShortMessage[] commands = buffer.poll();

            if (commands != null && MidiServer.midiOut != null) {
                Logger.getLogger(SyncSend.class.getName()).log(Level.FINE, "Buffer populated", (Object) null);
                // Logger.getLogger(SyncSend.class.getName()).log(Level.INFO, "Processing the buffer and sending to the midi output device", (Object) null);

                for (ShortMessage command : commands) {
                    try {
                        MidiServer.midiOut.sendMessage(command);
                        Logger.getLogger(SyncSend.class.getName()).log(Level.FINE,
                            "Server sending midi event: " + MidiServer.midiOut.getClass().getSimpleName() + " " +
                            command.getCommand() + " " + command.getData1() + " " + command.getData2(),
                            (Object) null);
                        Thread.sleep(0, 500_000); // Optional pacing
                    } catch (InterruptedException ex) {
                        Logger.getLogger(SyncSend.class.getName()).log(Level.FINE, "SyncSend interrupted during send", ex);
                        Thread.currentThread().interrupt(); // Preserve interrupt status
                    }
                }
            } else {
                try {
                    Thread.sleep(1); // Yield to avoid busy loop
                } catch (InterruptedException ex) {
                    Logger.getLogger(SyncSend.class.getName()).log(Level.FINE, "SyncSend interrupted during idle", ex);
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
