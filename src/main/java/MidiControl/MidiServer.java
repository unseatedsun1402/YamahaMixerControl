/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package MidiControl;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.*;

/**
 *
 * @author ethanblood
 */

class MidiServer implements MidiInterface, Runnable {
    static Receiver rcvr;
    static MidiDevice midiport;
    static Transmitter tx;
    static ConcurrentLinkedQueue<ShortMessage[]> buffer = new ConcurrentLinkedQueue<>();

    // Input MIDI buffer and receiver
    static ConcurrentLinkedQueue<MidiMessage> inputBuffer = new ConcurrentLinkedQueue<>();
    static MidiInputReceiver inputReceiver;

    public static void addtosendqueue(ShortMessage[] commands) {
        buffer.add(commands);
    }
    
    /**
     * Sets the receiver
     * @param dev
     * @throws MidiUnavailableException 
     */
    protected static void setReceiver(int dev) throws MidiUnavailableException{
        rcvr = MidiInterface.getReceiver(dev);
        if(!(midiport.isOpen())){
            midiport.open();
            System.out.println("Opened MIDI device for receiver: " + dev);
        }
        System.out.println("Receiver set for device: " + dev);
    }
    
    /**
     * Sets the Transmitter
     * @param dev
     * @throws MidiUnavailableException 
     */
    protected static void setTransmitter(int dev) throws MidiUnavailableException {
        tx = MidiInterface.getTransmitter(dev);
        if(!(midiport.isOpen())){
            midiport.open();
            System.out.println("Opened MIDI device for transmitter: " + dev);
        }
        // Attach input receiver to transmitter for incoming MIDI
        if (tx != null) {
            if (inputReceiver == null) {
                inputReceiver = new MidiInputReceiver(inputBuffer);
                System.out.println("Created new MidiInputReceiver");
            }
            tx.setReceiver(inputReceiver);
            System.out.println("Transmitter set and inputReceiver attached for device: " + dev);
        } else {
            System.out.println("Transmitter is null for device: " + dev);
        }
    }
    
    /**
     * Sets the MIDI output device (Receiver)
     * @param dev
     * @throws MidiUnavailableException
     */
    public static void setOutputDevice(int dev) throws MidiUnavailableException {
        midiport = MidiInterface.getMidiDevice(dev);
        Logger.getLogger(MidiServer.class.getName()).log(Level.INFO,"Setting output device: "+dev, (Object) null);
        if (midiport.getMaxReceivers() != 0) {
            setReceiver(dev);
            Logger.getLogger(MidiServer.class.getName()).log(Level.INFO,"Setting midi server receiver to device: "+dev, (Object) null);
            return;
        }
        Logger.getLogger(MidiServer.class.getName()).log(Level.WARNING,"Is an input interface: "+dev, (Object) null);
        return;
    }

    /**
     * Sets the MIDI input device (Transmitter)
     * @param dev
     * @throws MidiUnavailableException
     */
    public static void setInputDevice(int dev) throws MidiUnavailableException {
        midiport = MidiInterface.getMidiDevice(dev);
        Logger.getLogger(MidiServer.class.getName()).log(Level.INFO,"Setting input device: "+dev, (Object) null);
        if (midiport.getMaxTransmitters() != 0) {
            setTransmitter(dev);
            Logger.getLogger(MidiServer.class.getName()).log(Level.INFO,"Set midi server transmitter to device: "+dev, (Object) null);
            return;
        }
        else{
            setReceiver(dev);
            Logger.getLogger(MidiServer.class.getName()).log(Level.WARNING,"Is an output interface: "+dev, (Object) null);
            return;
        }
    }

    /**
     * Sets both input and output device (legacy)
     * @param dev
     * @throws MidiUnavailableException
     */
    public static void setDevice(int dev) throws MidiUnavailableException{
        midiport = MidiInterface.getMidiDevice(dev);
        Logger.getLogger(MidiServer.class.getName()).log(Level.INFO,"Setting legacy midi device: "+dev, (Object) null);
        if (midiport.getMaxReceivers() != 0) {
            setReceiver(dev);
            Logger.getLogger(MidiServer.class.getName()).log(Level.INFO,"Is an output interface: "+dev, (Object) null);
        }
        if (midiport.getMaxTransmitters() != 0) {
            Logger.getLogger(MidiServer.class.getName()).log(Level.INFO,"Is an input interface: "+dev, (Object) null);
            setTransmitter(dev);
        }
    }

    
    
    @Override
    public void run() {
        while (true) {
            // Process outgoing MIDI events
            SyncSend checkbuffer = new SyncSend(buffer, rcvr);
            Thread send = new Thread(checkbuffer);
            send.start();

            // Process incoming MIDI events
            processIncomingMidi();

            try {
                send.join();
                Thread.sleep(2); // avoid busy loop
            } catch (InterruptedException ex) {
                Logger.getLogger(MidiServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    // Example: process and print incoming MIDI events
    private void processIncomingMidi() {
        while (!inputBuffer.isEmpty()) {
            MidiMessage msg = inputBuffer.poll();
            if (msg != null) {
                System.out.println("Received MIDI: " + msg);
                // Optionally print message details
                if (msg instanceof ShortMessage) {
                    ShortMessage sm = (ShortMessage) msg;
                    System.out.println("Command: " + sm.getCommand() + ", Channel: " + sm.getChannel() +
                                       ", Data1: " + sm.getData1() + ", Data2: " + sm.getData2());
                }
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
    
    public static void main(String[] args) {
        try {
            int deviceIndex = 0; // Default device index, can be changed or parsed from args
            if (args.length > 0) {
                try {
                    deviceIndex = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid device index, using default 0.");
                }
            }
            setDevice(deviceIndex);
            MidiServer server = new MidiServer();
            Thread serverThread = new Thread(server);
            serverThread.start();
            System.out.println("MidiServer started on device index: " + deviceIndex);
        } catch (MidiUnavailableException e) {
            System.err.println("Failed to start MidiServer: " + e.getMessage());
        }
    }
}
