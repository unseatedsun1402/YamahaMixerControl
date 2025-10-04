package MidiControl;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.ShortMessage;

import javax.sound.midi.Receiver;

/**
 * Synchronized threaded send midi method
 * @author ethanblood
 */
public class SyncSend implements Runnable{
    protected final ConcurrentLinkedQueue<ShortMessage[]> buffer;
    protected final Receiver rcvr;
    protected int ThreadId;
    
    public SyncSend(ConcurrentLinkedQueue<ShortMessage[]> buff, Receiver rx){
        this.buffer = buff;
        this.ThreadId = 0;
        this.rcvr = rx;
    }

    @Override
    public synchronized void run() {
        Logger.getLogger(SyncSend.class.getName()).log(Level.INFO, "SyncSend thread started.", (Object) null);
        while (true) {
            if(!(this.buffer.isEmpty())){
                Logger.getLogger(SyncSend.class.getName()).log(Level.FINE, ("Buffer populated" + buffer.size()), (Object) null);
                if(!(this.rcvr == null)){
                    Logger.getLogger(SyncSend.class.getName()).log(Level.INFO, ("Processing the buffer and sending to the midi output device"), (Object) null);
                    for(int i = 0; i <this.buffer.size(); i ++){
                        ShortMessage[] commands = this.buffer.poll();
                        for(ShortMessage command : commands){
                            try {
                                this.rcvr.send(command, -1);
                                Logger.getLogger(SyncSend.class.getName()).log(Level.FINE, ("Server sending midi event: "+rcvr.toString()+" "+command.getCommand()+command.getData1()+command.getData2()), (Object) null);
                                Thread.sleep(0,500000); //- 0.5 ms delay between messages to avoid overwhelming the midi device
                            } catch (InterruptedException ex) {
                                Logger.getLogger(SyncSend.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                }
                else{
                    this.buffer.clear();
                    Logger.getLogger(SyncSend.class.getName()).log(Level.FINE, ("Buffer sent" + buffer.size()), (Object) null);
                }
            }
            else{
                try {
                    Thread.sleep(1); // avoid busy loop
                }
                catch (InterruptedException ex) {
                    Logger.getLogger(SyncSend.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        
        }
    
    }
}
