package MidiControl;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.*;

import MidiControl.Utilities.SysExMapping;
// import MidiControl.Utilities.SysExTableParser;
import MidiControl.Utilities.SysExParser;
import MidiControl.Utilities.NRPNDispatchTarget;
import MidiControl.Utilities.NrpnMapping;
import MidiControl.Utilities.NrpnParser;
import MidiControl.Utilities.NrpnRegistry;

/**
 *
 * @author ethanblood
 */

class MidiServer implements MidiInterface, NRPNDispatchTarget, Runnable {
    static Receiver rcvr;
    static MidiDevice midiport;
    static Transmitter tx;
    static final ConcurrentLinkedQueue<ShortMessage[]> buffer = new ConcurrentLinkedQueue<>();

    // Input MIDI buffer and receiver
    static final ConcurrentLinkedQueue<MidiMessage> inputBuffer = new ConcurrentLinkedQueue<>();
    static MidiInputReceiver inputReceiver;

    // SysExTableParser tableParser = new SysExTableParser();
    // List<SysExMapping> sysexMappings = tableParser.parse("ParamChangeList_V350_M7CL.xls", "Parameter Change");
    // SysExParser sysexDecoder = new SysExParser(sysexMappings);
    NrpnParser nrpnParser = new NrpnParser();
    NrpnRegistry nrpnRegistry = new NrpnRegistry();


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
        System.out.println("MidiServer Thread started");
        while (true) {
            processIncomingMidi();
            try {
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

            if (msg instanceof ShortMessage sm) {
                System.out.printf("MIDI: cmd=%d, data1=%d, data2=%d%n",sm.getCommand(), sm.getData1(), sm.getData2());
                nrpnParser.parse(sm); // Add this line
                //ParseMidi.printResolvedTypes(sm); // Optional

            }
            else if (msg instanceof SysexMessage sysex) {
                byte[] data = sysex.getData();
                Logger.getLogger(MidiServer.class.getName()).log(Level.INFO,"SysEx Raw Data: " + Arrays.toString(data));

                // Decode using your SysExParser
                // sysexDecoder.parse(data);
                }
            }
        }
    }

    /**
     * Handles a resolved NRPN by broadcasting it to all connected WebSocket clients.
     * @param id
     * @param param
     * @param value
     */
    public static void handleResolvedNRPN(String json) {
        Socket.broadcast(json);
    }


    
    public static void bufferMidi(String message) throws InterruptedException{
        ShortMessage[] commands = WebControlParser.parseJSON(message);
        Logger.getLogger(MidiServer.class.getName()).log(Level.INFO,"Buffering MIDI: " + Arrays.toString(commands));
        addtosendqueue(commands);
    }
    
    
    /**
     * Parses and sends a json string object as a midi command array
     * @param message
     * @throws InterruptedException 
     */
    public void sendMidi(String message) throws InterruptedException, MidiUnavailableException{
        if (!midiport.isOpen()) {
            midiport.open();
        }
        System.out.println("Receiver: " + rcvr.getClass().getName());
        if (rcvr != null) {
            try {
                ShortMessage[] commands = WebControlParser.parseJSON(message);
                for (ShortMessage command : commands) {
                    System.out.println("Sending: " + command.getCommand() + " " + command.getData1() + " " + command.getData2());
                    rcvr.send(command, -1);
                    Thread.sleep(1, 11);
                }
            } catch (Exception e) {
                System.out.println("Error - send midi: " + e);
            }
        }

            
    }
    
    public static void main(String[] args) {
        try {
            int deviceIndex = 0;
            if (args.length > 0) {
                try {
                    deviceIndex = Integer.parseInt(args[0]);
                }
                catch (NumberFormatException e) {
                    System.out.println("Invalid device index, using default 0.");
                }
            }
            setDevice(deviceIndex);
            MidiServer server = new MidiServer();
            Thread serverThread = new Thread(server);
            serverThread.start();

            SyncSend checkbuffer = new SyncSend(buffer, rcvr);
            Thread sendThread = new Thread(checkbuffer);
            sendThread.start();
            } 
        catch (MidiUnavailableException e) {
            System.err.println("Failed to start MidiServer: " + e.getMessage());
        }
    }

    @Override
    public void handleResolvedNRPN(NrpnMapping mapping, int value) {
        String json = String.format(
            "{\"type\":\"nrpnUpdate\",\"channelType\":\"%s\",\"channelIndex\":%d,\"controlType\":\"%s\",\"value\":%d}",
            mapping.channelType(), mapping.channelIndex(), mapping.controlType(), value
        );
        Socket.broadcast(json);
    }

}
