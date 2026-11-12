package MidiControl.MidiDeviceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.*;

import MidiControl.Server.MidiServer;
import MidiControl.Server.Rehydration.RehydrationManager;
import MidiControl.SystemTools.Format;
import MidiControl.Telemetry.SystemTelemetry;
import MidiControl.MidiDeviceManager.MidiSendEngine.ThroughputProfile;

public class MidiIOManager {

    private volatile MidiOutput midiOut;
    private volatile MidiInput  midiIn;
    private int        outPort = -1;
    private int        inPort = -1;
    private MidiServer server;
    private final Logger logger = Logger.getLogger(MidiIOManager.class.getName());
    private TransportMode mode;
    private static boolean debug;

    private final ArrayBlockingQueue<byte[]> sendQueue = new ArrayBlockingQueue<>(4096);

    private volatile MidiSendEngine sendEngine;
    private volatile ThroughputProfile throughput = ThroughputProfile.SAFE_DIN;

    public MidiIOManager(MidiServer server) {
        this.server = server;
        Settings settings = new ServerSettings();
        int inputDeviceIndex = settings.getInputDeviceIndex();
        int outputDeviceIndex = settings.getOutputDeviceIndex();

        if (inputDeviceIndex < 0 && outputDeviceIndex < 0) {
            logger.info("No previously saved MIDI ports; devices remain closed");
            return;
        }

        boolean inOk = inputDeviceIndex < 0 || trySetInputDevice(inputDeviceIndex);
        boolean outOk = outputDeviceIndex < 0 || trySetOutputDevice(outputDeviceIndex);
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

    public synchronized void shutdown() {
        logger.info("Shutting down MIDI devices.");
        if (sendEngine != null) sendEngine.stop();
        sendEngine = null;
        closeInputDevice();
        closeOutputDevice();
    }

    public void sendAsync(byte[] data) {
        enqueue(data, false);
    }

    public void sendPriorityAsync(byte[] data) {
        enqueue(data, true);
    }

    private void enqueue(byte[] data, boolean priority) {
        MidiSendEngine engine = sendEngine;
        if (midiOut == null || engine == null) {
            logger.warning("Attempted to send MIDI before output device was set.");
            return;
        }
        if(debug) logger.info(String.format("Outputing on %s - %s",getMidiOutName(),Format.bytesToHex(data)));
        boolean accepted = priority ? engine.offerPriority(data) : engine.offer(data);
        if (!accepted) {
            logger.warning(priority
                    ? "Priority MIDI queue full; priority message dropped."
                    : "Send queue full; message dropped or consider coalescing.");
        }
    }

    public synchronized boolean trySetOutputDevice(int index) {
        boolean ok = openOutputDevice(index);
        if (ok) logger.info("The current output device is: " + outPort);
        return ok;
    }

    public synchronized boolean trySetInputDevice(int index) {
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

    public synchronized void setThroughputProfile(ThroughputProfile profile) {
        if (throughput == profile) return;
        this.throughput = profile;
        RehydrationManager.changeRehydrationDelay(profile.pollDelay);
        if (sendEngine != null) sendEngine.setThroughputProfile(profile);
        logger.info("Throughput profile (pacing) set to " + profile);
    }

    public MidiOutput getMidiOut() { return midiOut; }
    public MidiInput getMidiIn() { return midiIn; }

    private List<MidiDevice.Info> listDevices() {
        return List.of(MidiDeviceUtils.getDeviceInfos());
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
                MidiDevice device = MidiDeviceUtils.getDevice(info);
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

    public synchronized boolean hasValidDevices() {
        return midiIn != null && midiOut != null && midiIn.isOpen() && midiOut.isOpen();
    }

    public MidiSendEngine.ThroughputProfile getThroughputProfile(){
        return throughput;
    }

    public CoalesceEngine getCoalesceEngine(){
        MidiSendEngine engine = sendEngine;
        return engine == null ? null : engine.getCoalesceEngine();
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
        closeInputDevice();

        // Close output
        closeOutputDevice();

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

        if (cachedInput < 0 || cachedOutput < 0) {
            logger.warning(String.format(
                    "MIDI reset skipped: no configured device pair (input=%d, output=%d)",
                    cachedInput, cachedOutput));
            return false;
        }

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
        closeOutputDevice();
        outPort = -1;

        MidiDevice device = null;
        MidiDevice.Info info = null;
        boolean wasOpen = false;
        try {
            info = MidiDeviceUtils.getInfo(index);
            logger.info("Opening MIDI output index " + index + ": " + info.getName());
            device = MidiDeviceUtils.getDevice(info);
            wasOpen = device.isOpen();
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
            if (device != null && !wasOpen && device.isOpen()) device.close();
            logger.log(Level.WARNING, "Failed to open MIDI output index " + index
                    + (info == null ? "" : " (" + info.getName() + ")"), e);
            return false;
        }
    }

    private boolean openInputDevice(int index) {
        MidiDevice device = null;
        MidiDevice.Info info = null;
        boolean wasOpen = false;
        try {
            if (midiIn != null) {
                closeInputDevice();
                logger.info("Closed previous input device and transmitter.");
            }
            inPort = -1;

            info = MidiDeviceUtils.getInfo(index);
            logger.info("Opening MIDI input index " + index + ": " + info.getName());
            device = MidiDeviceUtils.getDevice(info);
            wasOpen = device.isOpen();
            device.open();

            if (device.getMaxTransmitters() > 0 || device.getMaxTransmitters() == -1) {
                midiIn = new InputWrapper(device, server.getInputBuffer());
                inPort = index;

                attachIngressTelemetry();   // always attach here
                logger.info("Transmitter set for input device index: " + index);
                return true;
            }

            logger.warning("Device is output-only: " + index);
            if (!wasOpen) device.close();
            return false;

        } catch (MidiUnavailableException e) {
            if (device != null && !wasOpen && device.isOpen()) {
                device.close();
            }
            logger.log(Level.WARNING, "Failed to open MIDI input index " + index
                    + (info == null ? "" : " (" + info.getName() + ")"), e);
            return false;
        }

    }

    private boolean sharesDevice() {
        return midiIn instanceof InputWrapper input && midiOut instanceof ReceiverWrapper output
                && input.getDevice() == output.getDevice();
    }

    private void closeInputDevice() {
        if (midiIn == null) return;
        if (sharesDevice()) ((InputWrapper) midiIn).close(false);
        else midiIn.close();
        midiIn = null;
    }

    private void closeOutputDevice() {
        if (midiOut == null) return;
        if (sharesDevice()) ((ReceiverWrapper) midiOut).close(false);
        else midiOut.close();
        midiOut = null;
    }
}
