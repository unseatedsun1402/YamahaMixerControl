package MidiControl;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.*;

import MidiControl.ChannelMappings.ControlType;
import MidiControl.ControlMappings.buildFaderJson;
import MidiControl.ControlServer.InputHandler;
import MidiControl.Controls.ControlRegistry;
import MidiControl.MidiDeviceManager.MidiDeviceUtils;
import MidiControl.MidiDeviceManager.MidiInput;
import MidiControl.MidiDeviceManager.MidiOutput;
import MidiControl.MidiDeviceManager.MidiSettings;
import MidiControl.MidiDeviceManager.ReceiverWrapper;
import MidiControl.MidiDeviceManager.TransmitterWrapper;
import MidiControl.MidiDeviceManager.Configs.Settings;
import MidiControl.NrpnUtils.*;
import MidiControl.SysexUtils.*;
import jakarta.annotation.PreDestroy;

/**
 * Server for hosting a hardware device for midi communication and relaying this to web sockets to be broadcast to client sessions
 */
public class MidiServer implements MidiInterface, NRPNDispatchTarget, Runnable {
    public static MidiOutput midiOut;
    public static MidiInput midiIn;
    static final ConcurrentLinkedQueue<MidiMessage[]> outputBuffer = new ConcurrentLinkedQueue<>();
    static final ConcurrentLinkedQueue<MidiMessage> inputBuffer = new ConcurrentLinkedQueue<>();
    static final NrpnParser nrpnParser = new NrpnParser();
    static final InputHandler inputHandler = new InputHandler(nrpnParser);
    static final List<SysexMapping> mappings = SysexMappingLoader.loadMappings();
    static final SysexRegistry registry = new SysexRegistry(mappings);
    static final ControlRegistry controlRegistry = new ControlRegistry(mappings);
    private static final Logger logger = Logger.getLogger(MidiServer.class.getName());

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down MIDI devices.");
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

    public static void addtoinputqueue(MidiMessage msg) { // used for testing
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
    logger.info("Opening device: " + info.getName());
    Settings.updateOutputDevice(index, info.getName(), info.getDescription());
    Socket.restartSyncSend();
    }

    /**
     * Sets the MIDI input device (Transmitter)
     * @param dev
     * @throws MidiUnavailableException
     */
    public static void setInputDevice(int index) throws MidiUnavailableException {
        // Close previous input device if open
        if (midiIn != null) {
            midiIn.close();
            logger.info("Closed previous input device and transmitter.");
        }

        MidiDevice device = MidiDeviceUtils.getDevice(index);
        logger.info("Opening device: " + device.getDeviceInfo().getName());
        device.open(); // Required before querying transmitters

        logger.info("After open -> Transmitters: " + device.getMaxTransmitters());

        if (device.getMaxTransmitters() > 0 || device.getMaxTransmitters() == -1) {
            midiIn = new TransmitterWrapper(device, inputBuffer);
            logger.info("Transmitter set for device: " + index);
            MidiDevice.Info info = midiIn.getDeviceInfo();
            Settings.updateInputDevice(index, info.getName(), info.getDescription());
        } else {
            logger.warning("Device is output-only: " + index);
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
            logger.info("Loaded NRPN mappings from external file.");
        } else {
            NrpnRegistry.INSTANCE.loadFromClasspath(mappingPath);
            logger.info("Loaded NRPN mappings from classpath.");
        }
        logger.info("NrpnRegistry has " + NrpnRegistry.INSTANCE.getMappings().size() + " mappings loaded.");
    }
    
    @Override
    public void run() {
            logger.setLevel(Level.FINE);
            for (Handler h : Logger.getLogger("").getHandlers()) {
                h.setLevel(Level.FINE);
            }try {
            logger.info("MidiServer Thread started.");   
            initializeMappings();
            waitForMidiDevices();
            // restoreDevicesWithRetry();
            logDeviceStatus();

            Thread processingThread = new Thread(this::startProcessingLoop);
            processingThread.setName("MidiProcessingThread");
            processingThread.setPriority(Thread.NORM_PRIORITY + 1); // Slight boost
            processingThread.setUncaughtExceptionHandler((t, e) -> {
                Logger.getLogger(MidiServer.class.getName()).log(Level.SEVERE, "Processing thread crashed: " + t.getName(), e);
            });
            processingThread.start();
        } catch (Exception e) {
            Logger.getLogger(MidiServer.class.getName()).log(Level.SEVERE, "MidiServer thread crashed", e);
        }
    }

    private void waitForMidiDevices() {
        int retries = 10;
        while (MidiSystem.getMidiDeviceInfo().length < 1 && retries-- > 0) {
            logger.info("Waiting for MIDI devices...");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                logger.warning("Sleep interrupted while waiting for MIDI devices");
            }
        }
    }

    public boolean restoreDevicesWithRetry() {
        
        int retries = 6;
        while (retries-- > 0) {
            logger.info("midiIn = " + midiIn);
            logger.info("Available devices: " + MidiSystem.getMidiDeviceInfo().length);
            if (MidiServer.midiOut != null && MidiServer.midiOut.getDeviceInfo() != null) {
                logger.log(Level.INFO, "midiOut is ready — restored devices.");
                Settings.restore(Settings.getSettingsOrDefault());
                return true;
            }

            logger.log(Level.INFO, "Waiting for midiOut initialization...");
            try {
                Thread.sleep(750);
                Settings.restore(Settings.getSettingsOrDefault());
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "Interrupted during restore wait", e);
                break;
            }
        }
        logger.log(Level.WARNING, "midiOut not initialized — skipping restore.");return false;
    }


    private void logDeviceStatus() {
        logger.info("Receiver attached: " + (midiIn != null));
        logger.info("Transmitter attached: " + (midiOut != null));
    }

    private void startProcessingLoop() {
        long lastHeartbeat = System.currentTimeMillis();
        while (true) {
            processIncomingMidi();
            if (System.currentTimeMillis() - lastHeartbeat > 5000) {
                logger.info("Processing loop heartbeat: still alive");
                lastHeartbeat = System.currentTimeMillis();
            }
            try {
                Thread.sleep(2);
            } catch (InterruptedException ex) {
                logger.log(Level.SEVERE, "Processing loop interrupted", ex);
                break;
            }
        }
    }

    public void processIncomingMidi() {
        while (!inputBuffer.isEmpty()) {
            MidiMessage msg = inputBuffer.poll();
            if (msg == null) continue;
            logger.fine("Received MIDI: " + SysexParser.bytesToHex(msg.getMessage()));

            Object event = inputHandler.handle(msg);
            if (event == null) continue;

            if (event instanceof byte[]) {
                byte[] sysex = (byte[]) event;
                logger.finer("Raw Sysex: " + SysexParser.bytesToHex(sysex));
                MidiControl.Controls.Control control = controlRegistry.resolveSysex(sysex);
                if (control != null) {
                    logger.info("Resolved Control: " + control.getControlGroup());
                } else {
                    logger.info("No Control mapping found for Sysex message.");
                }
            } else if (event instanceof ShortMessage) {
                nrpnParser.parse((ShortMessage) event);
            } else {
                logger.info("Unhandled MIDI event type: " + event.getClass().getName());
            }
        }
    }

    /**
     * Handles a resolved NRPN by broadcasting it to all connected WebSocket clients.
     * @param id
     * @param param
     * @param value
     */
    public void handleResolvedNRPN(String json) {
        logger.info("HANDLE NRPN CALLED on instance: " + this);
        Socket.broadcast(json);
    }
    
    public static void bufferMidi(String message) throws InterruptedException{
        ShortMessage[] commands = WebControlParser.parseJSON(message);
        Logger.getLogger(MidiServer.class.getName()).log(Level.FINE,"Buffering Output MIDI: " + SysexParser.bytesToHex(message.getBytes()));
        addtosendqueue(commands);
    }
    
    public void sendMidi(String message) throws InterruptedException, MidiUnavailableException {
        if (midiOut == null) {
            logger.warning("No MIDI output device set. Cannot send message.");
            return;
        }

        ShortMessage[] commands;
        if (message.trim().startsWith("[")) {
            // Raw MIDI array
            commands = ParseMidi.parseRawMidiJson(message);
        } else {
            commands = WebControlParser.parseJSON(message);
        }

        for (ShortMessage command : commands) {
            midiOut.sendMessage(command);
            Thread.sleep(1, 11);
        }
    }

    public static void main(String[] args) {
    try {
        logger.info("Resolved user.home: " + System.getProperty("user.home")); //get settings from file
        MidiSettings settings = Settings.getSettings(); // Load cached settings
        int retries = 10;
        while (MidiSystem.getMidiDeviceInfo().length < 1 && retries-- > 0) {
            logger.info("Waiting for MIDI devices...");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                logger.warning("Sleep failed Waiting for midi devices");
                e.printStackTrace();
            }
        }

        // int inputIndex = settings != null ? Settings.getLastInput() : 0;
        // int outputIndex = settings != null ? Settings.getLastOutput() : 0;
        // MidiControl.ListDevices.listDevices(); // List available devices
        int inputIndex = 13; //hard set for testing
        int outputIndex = 4; //hard set for testing
        configureIO(inputIndex, outputIndex);
        logger.info("Starting MidiServer with input=" + inputIndex + ", output=" + outputIndex);

        // Build and export mappings only in standalone mode
        NrpnRegistry.INSTANCE.buildProfileMapping();
        logger.info("Built NRPN profile mapping.");
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
        logger.info(String.format("Received dispatch from %s channel %d",mapping.label(),mapping.channelIndex()));
        String json;
        switch (mapping.controlType()) {
            case OUTPUT_FADER:
                json = buildFaderJson.buildOutputFaderJson(mapping, value);
                break;

            case FADER:
                json = buildFaderJson.buildInputFaderJson(mapping, value);
                break;

            default:
                // Generic fallback for all other control types
                json = "{\"nrpn\":\"" + mapping.label() + "\",\"value\":" + value + "}";
                break;
        }
        Socket.broadcast(json);
    }
}
