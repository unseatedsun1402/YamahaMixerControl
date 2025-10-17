package MidiControl;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.*;

import MidiControl.ChannelMappings.ControlType;
import MidiControl.ControlMappings.buildFaderJson;
import MidiControl.MidiDeviceManager.MidiDeviceUtils;
import MidiControl.MidiDeviceManager.MidiInput;
import MidiControl.MidiDeviceManager.MidiOutput;
import MidiControl.MidiDeviceManager.ReceiverWrapper;
import MidiControl.MidiDeviceManager.TransmitterWrapper;
import MidiControl.Utilities.NRPNDispatchTarget;
import MidiControl.Utilities.NrpnMapping;
import MidiControl.Utilities.NrpnParser;
import MidiControl.Utilities.NrpnRegistry;

/**
 *
 * @author ethanblood
 */
public class MidiServer implements MidiInterface, NRPNDispatchTarget, Runnable {
    public static MidiOutput midiOut;
    public static MidiInput midiIn;
    static final ConcurrentLinkedQueue<ShortMessage[]> outputBuffer = new ConcurrentLinkedQueue<>();
    static final ConcurrentLinkedQueue<MidiMessage> inputBuffer = new ConcurrentLinkedQueue<>();
    NrpnParser nrpnParser = new NrpnParser();

    public static int getBufferSize() {
        return outputBuffer.size();
    }

    public static void addtosendqueue(ShortMessage[] commands) {
       outputBuffer.add(commands);
    }

    public static void addtoinputqueue(ShortMessage msg) { // used for testing
        inputBuffer.add(msg);
    }

    public static void clearOutputBuffer() {
        outputBuffer.clear();
    }

    public static void clearInputBuffer() {
        inputBuffer.clear();
    }

    
    public static Receiver getReceiver() {  // used for testing
        if (midiOut instanceof ReceiverWrapper wrapper) {
            return wrapper.getRawReceiver();
        }
        return null;
    }

    public static Transmitter getTransmitter() {  //used for testing
    if (midiIn instanceof TransmitterWrapper wrapper) {
        return wrapper.getRawTransmitter();
        }
        return null;
    }

    /**
     * Sets the MIDI output device (Receiver)
     * @param dev
     * @throws MidiUnavailableException
    //  */
    public static void setOutputDevice(int index) throws MidiUnavailableException {
    midiOut = new ReceiverWrapper(MidiDeviceUtils.getDevice(index));
    Socket.restartSyncSend();
    }


    /**
     * Sets the MIDI input device (Transmitter)
     * @param dev
     * @throws MidiUnavailableException
     */
    public static void setInputDevice(int index) throws MidiUnavailableException {
        MidiDevice device = MidiDeviceUtils.getDevice(index);
        device.open(); // Required before querying transmitters
            Logger.getLogger(MidiServer.class.getName()).log(Level.INFO,
                "After open → Transmitters: " + device.getMaxTransmitters());
        if (device.getMaxTransmitters() != 0) {
            midiIn = new TransmitterWrapper(device, inputBuffer);
            Logger.getLogger(MidiServer.class.getName()).log(Level.INFO, "Transmitter set for device: " + index);
        } else {
            Logger.getLogger(MidiServer.class.getName()).log(Level.WARNING, "Device is output-only: " + index);
            // Do NOT overwrite midiOut here
        }
    }

    //Support for batch IO config
    public static void configureIO(int inputIndex, int outputIndex) throws MidiUnavailableException {
        setInputDevice(inputIndex);
        setOutputDevice(outputIndex);
    }

    
    @Override
    public void run() {
        System.out.println("MidiServer Thread started from run()");

        String mappingPath = "MidiControl/nrpn_mappings.json";
        File mappingFile = new File(mappingPath);

        if (mappingFile.exists()) {
            NrpnRegistry.INSTANCE.loadFromJson(mappingPath);
            System.out.println("Loaded NRPN mappings from external file.");
        } else {
            NrpnRegistry.INSTANCE.loadFromClasspath(mappingPath);
        }
        System.out.println("NrpnRegistry has " + NrpnRegistry.INSTANCE.getMappings().size() + " mappings loaded.");

        
        NrpnMapping m = NrpnRegistry.INSTANCE.lookup(0, 126);
        System.out.println("Test mapping load 0, 126: " + m);
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
    public void processIncomingMidi() {
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
     * Direct MIDI dispatch from JSON — used for testing or manual control.
     * Prefer bufferMidi() for production use.
     */
    public void sendMidi(String message) throws InterruptedException, MidiUnavailableException {
    if (midiOut == null) {
        Logger.getLogger(MidiServer.class.getName()).log(Level.WARNING, "No MIDI output device set. Cannot send message.");
        return;
    }

    ShortMessage[] commands;
    if (message.trim().startsWith("[")) {
        // Raw MIDI array
        commands = ParseMidi.parseRawMidiJson(message);
    } else {
        // NRPN-style control
        commands = WebControlParser.parseJSON(message);
    }

    for (ShortMessage command : commands) {
        midiOut.sendMessage(command);
        Thread.sleep(1, 11);
    }
}

    
    public static void main(String[] args) {
    try {
        int deviceIndex = 0;
        if (args.length > 0) {
            try {
                deviceIndex = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid device index, using default 0.");
            }
        }

        configureIO(deviceIndex,deviceIndex);

        Logger.getLogger(MidiServer.class.getName()).log(Level.INFO, "Starting MidiServer on device index: " + deviceIndex);

        // Build and export mappings only in standalone mode
        NrpnRegistry.INSTANCE.buildProfileMapping();
        Logger.getLogger(MidiServer.class.getName()).log(Level.INFO, "Built NRPN profile mapping.");
        NrpnRegistry.INSTANCE.exportToJson("nrpn_mappings.json");

        // Start server and sync threads
        MidiServer server = new MidiServer();
        Thread serverThread = new Thread(server);
        serverThread.start();

        SyncSend sender = new SyncSend(MidiServer.outputBuffer);
        Thread sendThread = new Thread(sender);
        sendThread.start();

    } catch (MidiUnavailableException e) {
        System.err.println("Failed to start MidiServer: " + e.getMessage());
    }
}

    @Override
    public void handleResolvedNRPN(NrpnMapping mapping, int value) {
        if (mapping.controlType() == ControlType.OUTPUT_FADER) {
            Socket.broadcast(buildFaderJson.buildOutputFaderJson(mapping, value));
            return;
        }
        if (mapping.controlType() == ControlType.FADER){
            Socket.broadcast(buildFaderJson.buildInputFaderJson(mapping, value));
            Logger.getLogger(MidiServer.class.getName()).log(Level.INFO, String.format("Broadcasting NRPN JSON to MSB:%d LSB:%d Channel:$d Value:",mapping.msb(),mapping.lsb(),mapping.channelIndex()),value);
            return;
        }
        
    }
}
