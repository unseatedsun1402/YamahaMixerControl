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
import MidiControl.MidiDeviceManager.MidiSettings;
import MidiControl.MidiDeviceManager.ReceiverWrapper;
import MidiControl.MidiDeviceManager.TransmitterWrapper;
import MidiControl.MidiDeviceManager.Configs.Settings;
import MidiControl.NrpnUtils.NRPNDispatchTarget;
import MidiControl.NrpnUtils.NrpnMapping;
import MidiControl.NrpnUtils.NrpnParser;
import MidiControl.NrpnUtils.NrpnRegistry;
import jakarta.annotation.PreDestroy;

/**
 * Server for hosting a hardware device for midi communication and relaying this to web sockets to be broadcast to client sessions
 */
public class MidiServer implements MidiInterface, NRPNDispatchTarget, Runnable {
    public static MidiOutput midiOut;
    public static MidiInput midiIn;
    static final ConcurrentLinkedQueue<ShortMessage[]> outputBuffer = new ConcurrentLinkedQueue<>();
    static final ConcurrentLinkedQueue<MidiMessage> inputBuffer = new ConcurrentLinkedQueue<>();
    NrpnParser nrpnParser = new NrpnParser();

    @PreDestroy
    public void shutdown() {
        Logger.getLogger(MidiServer.class.getName()).log(Level.INFO, "Shutting down MIDI devices.");
        if (midiIn != null && midiIn.isOpen()) {
            midiIn.close();
        }
        if (midiOut != null && midiOut.isOpen()) {
            midiOut.close();
        }
    }

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
    MidiDevice.Info info = MidiServer.midiOut.getDeviceInfo();
    Settings.updateOutputDevice(index, info.getName(), info.getDescription());
    Socket.restartSyncSend();
    }

    /**
     * Sets the MIDI input device (Transmitter)
     * @param dev
     * @throws MidiUnavailableException
     */
    public static void setInputDevice(int index) throws MidiUnavailableException {
        MidiDevice device = MidiDeviceUtils.getDevice(index);
        device.open(); // Required BeforeEach querying transmitters
            Logger.getLogger(MidiServer.class.getName()).log(Level.INFO,
                "After open -> Transmitters: " + device.getMaxTransmitters());
        if (device.getMaxTransmitters() > 0 || device.getMaxTransmitters() == -1) {
            midiIn = new TransmitterWrapper(device, inputBuffer);
            Logger.getLogger(MidiServer.class.getName()).log(Level.INFO, "Transmitter set for device: " + index);
            MidiDevice.Info info = MidiServer.midiIn.getDeviceInfo();
            Settings.updateInputDevice(index, info.getName(), info.getDescription());
        } else {
            Logger.getLogger(MidiServer.class.getName()).log(Level.WARNING, "Device is output-only: " + index);
        }
    }

    //Support for batch IO config
    public static void configureIO(int inputIndex, int outputIndex) throws MidiUnavailableException {
        setInputDevice(inputIndex);
        setOutputDevice(outputIndex);
        Settings.saveCurrentIO(); // Persist current config
    }

    private void initializeMappings() {
        String mappingPath = "MidiControl/nrpn_mappings.json";
        File mappingFile = new File(mappingPath);

        if (mappingFile.exists()) {
            NrpnRegistry.INSTANCE.loadFromJson(mappingPath);
            Logger.getLogger(MidiServer.class.getName()).info("Loaded NRPN mappings from external file.");
        } else {
            NrpnRegistry.INSTANCE.loadFromClasspath(mappingPath);
            Logger.getLogger(MidiServer.class.getName()).info("Loaded NRPN mappings from classpath.");
        }

        Logger.getLogger(MidiServer.class.getName()).info("NrpnRegistry has " + NrpnRegistry.INSTANCE.getMappings().size() + " mappings loaded.");
    }

    
   @Override
    public void run() {
        try {
            Logger.getLogger(MidiServer.class.getName()).info("MidiServer Thread started.");
            initializeMappings();
            waitForMidiDevices();
            restoreDevices();
            logDeviceStatus();
        } catch (Exception e) {
            Logger.getLogger(MidiServer.class.getName()).log(Level.SEVERE, "MidiServer thread crashed", e);
        }

        startProcessingLoop();
    }

    private void waitForMidiDevices() {
        int retries = 10;
        while (MidiSystem.getMidiDeviceInfo().length < 1 && retries-- > 0) {
            Logger.getLogger(MidiServer.class.getName()).info("Waiting for MIDI devices...");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Logger.getLogger(MidiServer.class.getName()).warning("Sleep interrupted while waiting for MIDI devices");
            }
        }
    }

    private void restoreDevices() {
        try {
            setInputDevice(Settings.getLastInput());
            Logger.getLogger(MidiServer.class.getName()).info("Restored input device: " + midiOut.getDeviceInfo().getName());
        } catch (MidiUnavailableException e) {
            Logger.getLogger(MidiServer.class.getName()).warning("Last Input Device not available");
        }

        try {
            setOutputDevice(Settings.getLastOutput());
            Logger.getLogger(MidiServer.class.getName()).info("Restored output device: " + midiOut.getDeviceInfo().getName());
        } catch (MidiUnavailableException e) {
            Logger.getLogger(MidiServer.class.getName()).warning("Last Output Device not available");
        }
    }

    private void logDeviceStatus() {
        Logger.getLogger(MidiServer.class.getName()).info("Receiver attached: " + (midiIn != null));
        Logger.getLogger(MidiServer.class.getName()).info("Transmitter attached: " + (midiOut != null));
    }


    private void startProcessingLoop() {
        while (true) {
            processIncomingMidi();
            try {
                Thread.sleep(2);
            } catch (InterruptedException ex) {
                Logger.getLogger(MidiServer.class.getName()).log(Level.SEVERE, "Processing loop interrupted", ex);
            }
        }
    }

    // Example: process and print incoming MIDI events
    public void processIncomingMidi() {
    while (!inputBuffer.isEmpty()) {
        MidiMessage msg = inputBuffer.poll();
        if (msg != null) {
            Logger.getLogger(MidiServer.class.getName()).info("Received MIDI: " + msg);
            if (msg instanceof ShortMessage sm) {
                System.out.printf("MIDI: cmd=%d, data1=%d, data2=%d%n",sm.getCommand(), sm.getData1(), sm.getData2());
                nrpnParser.parse(sm); // Add this line
                // ParseMidi.printResolvedTypes(sm); // Optional
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
        Logger.getLogger(MidiServer.class.getName()).log(Level.FINE,"Buffering MIDI: " + Arrays.toString(commands));
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
        Logger.getLogger(Settings.class.getName()).log(Level.INFO,"Resolved user.home: " + System.getProperty("user.home")); //get settings from file
        MidiSettings settings = Settings.getSettings(); // Load cached settings
        int retries = 10;
        while (MidiSystem.getMidiDeviceInfo().length < 1 && retries-- > 0) {
            Logger.getLogger(MidiServer.class.getName()).log(Level.INFO, "Waiting for MIDI devices...");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Logger.getLogger(MidiServer.class.getName()).warning("Sleep failed Waiting for midi devices");
                e.printStackTrace();
            }
        }

        int inputIndex = settings != null ? Settings.getLastInput() : 0;
        int outputIndex = settings != null ? Settings.getLastOutput() : 0;

        configureIO(inputIndex, outputIndex);
        Logger.getLogger(MidiServer.class.getName()).log(Level.INFO,
            "Starting MidiServer with input=" + inputIndex + ", output=" + outputIndex);

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
            Logger.getLogger(MidiServer.class.getName()).log(Level.FINE, String.format("Broadcasting NRPN JSON to MSB:%d LSB:%d Channel:$d Value:",mapping.msb(),mapping.lsb(),mapping.channelIndex()),value);
            return;
        }
    }
}
