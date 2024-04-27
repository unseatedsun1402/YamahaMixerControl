/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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
    protected ConcurrentLinkedQueue<ShortMessage[]> buffer;
    protected Receiver rcvr;
    protected int ThreadId;
    
    public SyncSend(ConcurrentLinkedQueue<ShortMessage[]> buff, Receiver rx){
        this.buffer = buff;
        this.ThreadId = 0;
        this.rcvr = rx;
    }

    @Override
    public synchronized void run() {
        
        if(!(this.buffer.isEmpty())){
            //System.out.println("buffer populated" + buffer.size());
            if(!(this.rcvr == null)){
                for(int i = 0; i <this.buffer.size(); i ++){
                    ShortMessage[] commands = this.buffer.poll();
                    for(ShortMessage command : commands){
                        try {
                            this.rcvr.send(command, -1);
                            //System.out.println("Device "+rcvr.toString()+" "+command.getCommand()+command.getData1()+command.getData2());
                            Thread.sleep(0,500000);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(SyncSend.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
            else{
                this.buffer.clear();
            }
        }
    //System.out.println("buffer empty");
    }
    
}
