package MidiControl.MidiDeviceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;
import javax.sound.midi.*;

import MidiControl.Server.MidiServer;
import MidiControl.Server.Rehydration.RehydrationManager;
import MidiControl.SystemTools.Format;
import MidiControl.Telemetry.SystemTelemetry;
import MidiControl.MidiDeviceManager.MidiSendEngine.ThroughputProfile;

public class MidiIOManager {

    private MidiOutput midiOut;
    private MidiInput  midiIn;
    private int        outPort;
    private int        inPort;
    private MidiServer server;
    private final Logger logger = Logger.getLogger(MidiIOManager.class.getName());
    private TransportMode mode;
    private static boolean debug;

    private final ArrayBlockingQueue<byte[]> sendQueue = new ArrayBlockingQueue<>(4096);

    private MidiSendEngine sendEngine;
    private volatile ThroughputProfile throughput = ThroughputProfile.SAFE_DIN;

    public MidiIOManager(MidiServer server) {
        this.server = server;
        Settings settings = new ServerSettings();
        boolean inOk  = trySetInputDevice(settings.getInputDeviceIndex());
        boolean outOk = trySetOutputDevice(settings.getOutputDeviceIndex());
        if (inOk && outOk) logger.info("Loaded previous midi ports from settings");
        else logger.warning("loading settings failed: " + settings.toJson());
        logger.info("MidiIOManger available");
    }

    public MidiIOManager() { logger.info("MidiIOManger available for tests");  }

    public void setMidiOutForTest(MidiOutput mock) {
        this.midiOut = mock;
        this.sendEngine = new MidiSendEngine(midiOut, 2048, 4096);
        this.sendEngine.setThroughputProfile(throughput);
        this.sendEngine.start();
    }

    public void shutdown() {
        logger.info("Shutting down MIDI devices.");
        if (sendEngine != null) sendEngine.stop();
        if (midiIn != null && midiIn.isOpen()) midiIn.close();
        if (midiOut != null && midiOut.isOpen()) midiOut.close();
    }

    public void sendAsync(byte[] data) {
        if (midiOut == null || sendEngine == null) {
            logger.warning("Attempted to send MIDI before output device was set.");
            return;
        }
        if(debug) logger.info(String.format("Outputing on %s - %s",getMidiOutName(),Format.bytesToHex(data)));
        if (!sendEngine.offer(data)) {
            logger.warning("Send queue full; message dropped or consider coalescing.");
        }
    }

    public boolean trySetOutputDevice(int index) {
        boolean ok = openOutputDevice(index);
        if (ok) logger.info("The current output device is: " + outPort);
        return ok;
    }

    public boolean trySetInputDevice(int index) {
        boolean ok = openInputDevice(index);
        if (ok) logger.info("The current input device is: " + inPort);
        return ok;
    }

    public TransportMode getTransportMode() {
        return (mode != null) ? mode : TransportMode.SYSEX;
    }
    public TransportMode setTransportMode(TransportMode changeTo) {
        return this.mode = changeTo;
    }

    public void setThroughputProfile(ThroughputProfile profile) {
        ThroughputProfile current = this.getThroughputProfile();
        if(profile == ThroughputProfile.SAFE_DIN)server.getRehydrationManager().delayMeterRequests();
        if (current == profile) return;
        this.throughput = profile;
        RehydrationManager.changeRehydrationDelay(profile.pollDelay);
        if (sendEngine != null) sendEngine.setThroughputProfile(profile);
        logger.info("Throughput profile (pacing) set to " + profile);
    }

    public MidiOutput getMidiOut() { return midiOut; }
    public MidiInput getMidiIn() { return midiIn; }

    private List<MidiDevice.Info> listDevices() {
        return Arrays.asList(MidiSystem.getMidiDeviceInfo());
    }

    public List<MidiDeviceDTO> listDeviceDTOs() {
        List<MidiDevice.Info> infos = listDevices();
        List<MidiDeviceDTO> list = new ArrayList<>();
        for (int i = 0; i < infos.size(); i++) {
            MidiDevice.Info info = infos.get(i);
            MidiDeviceDTO dto = new MidiDeviceDTO();
            dto.id = String.valueOf(i);
            dto.name = info.getName();
            dto.description = info.getDescription();
            dto.vendor = info.getVendor();
            dto.version = info.getVersion();
            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                dto.canInput = (device.getMaxTransmitters() != 0);
                dto.canOutput = (device.getMaxReceivers() != 0);
            } catch (Exception e) {
                dto.canInput = false;
                dto.canOutput = false;
            }
            list.add(dto);
        }
        return list;
    }

    public boolean hasValidDevices() {
        return midiIn != null && midiOut != null && midiIn.isOpen() && midiOut.isOpen();
    }

    public MidiSendEngine.ThroughputProfile getThroughputProfile(){
        if (sendEngine == null) throw new IllegalStateException("MIDI output not initialised (sendEngine is null)");
        return sendEngine.getThroughputProfile();
    }

    public CoalesceEngine getCoalesceEngine(){
        if(sendEngine == null) return null;
        return this.sendEngine.getCoalesceEngine();
    }

    public RehydrationManager getRehydrationManager(){
        return this.server.getRehydrationManager();
    }

    public String getMidiOutName(){
        if(midiOut != null){
        return midiOut.getDeviceInfo().getName();
        }
        return "Device not set";
    }

    public String getMidiInName(){
        if (midiIn != null) {
            return midiIn.getDeviceInfo().getName();
        }
        return "Device not set";
    }

    public static void enableDebug(){
        debug = true;
    }
    
    public synchronized void hardReset() {
        logger.warning("Hard MIDI reset triggered");

        // Stop send engine
        if (sendEngine != null) {
            sendEngine.stop();
            sendEngine = null;
        }

        // Close input
        if (midiIn != null && midiIn.isOpen()) {
            midiIn.close();
            midiIn = null;
        }

        // Close output
        if (midiOut != null && midiOut.isOpen()) {
            midiOut.close();
            midiOut = null;
        }

        // Clear ports
        inPort = -1;
        outPort = -1;
    }

    public boolean rebuildDevices(int inIndex, int outIndex) {
        logger.info("Rebuilding MIDI devices");

        boolean inOk  = trySetInputDevice(inIndex);
        boolean outOk = trySetOutputDevice(outIndex);

        attachIngressTelemetry();

        return inOk && outOk;
    }

    public synchronized boolean resetMidiSubsystem() {
        int cachedInput  = inPort;
        int cachedOutput = outPort;

        logger.warning(String.format(
            "Resetting MIDI subsystem (cached in=%d, out=%d)", cachedInput, cachedOutput
        ));

        hardReset();

        boolean ok = rebuildDevices(cachedInput, cachedOutput);

        if (!ok) {
            logger.severe("MIDI subsystem reset failed; devices not valid after rebuild");
        }

        return ok;
    }

    private void attachIngressTelemetry() {
        if (midiIn != null && sendEngine != null) {
            midiIn.getInputReceiver().setIngressListener(sendEngine);
            logger.info("Ingress telemetry attached (post-reset)");
        } else {
            logger.warning("Ingress telemetry NOT attached (post-reset)");
        }
    }

    private boolean openOutputDevice(int index) {
        SystemTelemetry systemTelemetry = server != null ? server.getSystemTelemetry() : null;

        // Stop old engine
        if (sendEngine != null) {
            if (systemTelemetry != null) systemTelemetry.stop();
            sendEngine.stop();
            sendEngine = null;
        }

        try {
            MidiDevice device = MidiDeviceUtils.getDevice(index);
            midiOut = new ReceiverWrapper(device);
            logger.info("Opening output device: " + midiOut.getDeviceInfo().getName());
            outPort = index;

            sendEngine = new MidiSendEngine(midiOut, 2048, 4096);
            sendEngine.setThroughputProfile(throughput);

            if (systemTelemetry != null) {
                systemTelemetry.registerMidiTelemetry(sendEngine.getTelemetry());
                systemTelemetry.start();
            }

            sendEngine.start();
            return true;

        } catch (MidiUnavailableException e) {
            logger.warning("Failed to open output device: " + e.getMessage());
            return false;
        }
    }

    private boolean openInputDevice(int index) {
        try {
            if (midiIn != null) {
                midiIn.close();
                logger.info("Closed previous input device and transmitter.");
            }

            MidiDevice device = MidiDeviceUtils.getDevice(index);
            logger.info("Opening input device: " + device.getDeviceInfo().getName());
            device.open();

            if (device.getMaxTransmitters() > 0 || device.getMaxTransmitters() == -1) {
                midiIn = new InputWrapper(device, server.getInputBuffer());
                inPort = index;

                attachIngressTelemetry();   // always attach here
                logger.info("Transmitter set for input device index: " + index);
                return true;
            }

            logger.warning("Device is output-only: " + index);
            return false;

        } catch (MidiUnavailableException e) {
            logger.warning("Failed to open input device: " + e.getMessage());
            return false;
        }
    }
}
