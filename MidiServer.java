/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package MidiControl;
import java.io.IOException;
import java.math.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.*;

/**
 *
 * @author ethanblood
 */

class MidiServer implements MidiInterface,Runnable
{
    static Receiver rcvr;
    static MidiDevice midiport;
    static Transmitter tx;
    static ConcurrentLinkedQueue<ShortMessage[]> buffer = new ConcurrentLinkedQueue<>();
    static Settings conf = new Settings();
    
    public static void addtosendqueue(ShortMessage[] commands){
        buffer.add(commands);
    }
    
    /**
     * Sets the receiver
     * @param dev
     * @throws MidiUnavailableException 
     */
    protected static void setReceiver(int dev) throws MidiUnavailableException{
        rcvr = MidiInterface.getReceiver(dev);
        Logger.getLogger(MidiServer.class.getName()).log(Level.INFO, "Device out set to "+dev);
        if(!(midiport.isOpen())){
            midiport.open();
        }
    }
    
    /**
     * Sets the Transmitter
     * @param dev
     * @throws MidiUnavailableException 
     */
    protected static void setTransmitter(int dev) throws MidiUnavailableException{
        tx = MidiInterface.getTransmitter(dev);
        Logger.getLogger(MidiServer.class.getName()).log(Level.INFO, "Device in set to "+dev);
        
    }
    
    /**
     * Sets the class static variable device tx, rx
     * @param dev
     * @throws MidiUnavailableException 
     */
    public static void setDevice(int dev) throws MidiUnavailableException{
        midiport = MidiInterface.getMidiDevice(dev);
        try {
            if(!conf.checkChanges(dev, dev)){
                conf.writeSettings(dev,dev);
            }
        } catch (IOException ex) {
            Logger.getLogger(MidiServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        if(!(midiport.getMaxReceivers() == 0)){
            setReceiver(dev);
        }
        if(!(midiport.getMaxTransmitters() == 0))
        {
            setTransmitter(dev);
        }
    }
    
    @Override
    public void run(){
        
        try {
            int[] device = conf.readSettings();
            setDevice(device[0]);
        } catch (IOException ex) {
            Logger.getLogger(MidiServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MidiUnavailableException ex) {
            Logger.getLogger(MidiServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        while(true){
            SyncSend checkbuffer = new SyncSend(buffer,rcvr);
            Thread send = new Thread(checkbuffer);
            send.start();
            try {
                send.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(MidiServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public static void bufferMidi(String message) throws InterruptedException{
        ShortMessage[] commands = WebControlParser.parseJSON(message);
        addtosendqueue(commands);
    }
    
    
    /**
     * Parses and sends a json string object as a midi command array
     * @param message
     * @throws InterruptedException 
     */
    public void sendMidi(String message) throws InterruptedException, MidiUnavailableException{
        if(!(midiport.isOpen())){
            midiport.open();
        }
        else{
            if(!(rcvr == null)){
            try
            {
               ShortMessage[] commands = WebControlParser.parseJSON(message);
               for(ShortMessage command : commands){
                   System.out.println(command.getCommand()+" "+command.getData1()+" "+command.getData2());
                   rcvr.send(command,-1);
                   Thread.sleep(1,11);
               }
            }
            catch (Exception e)
                    {
                        System.out.println("Error - send midi: "+e);
                    }
            }
        }
            
    }
}
