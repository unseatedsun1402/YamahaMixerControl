package MidiControl.MidiDeviceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;
import javax.sound.midi.*;

import MidiControl.Server.MidiServer;
import MidiControl.Server.RehydrationManager;
import MidiControl.MidiDeviceManager.MidiSendEngine.ThroughputProfile;
import MidiControl.Routing.WebSocketEndpoint;

public class MidiIOManager {

    private MidiOutput midiOut;
    private MidiInput  midiIn;
    private int        outPort;
    private int        inPort;
    private MidiServer server;
    private final Logger logger = Logger.getLogger(MidiIOManager.class.getName());
    private TransportMode mode;

    // Keep your queue if you want producer-side coalescing later; the engine has its own bounded queues.
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

    public MidiIOManager() { logger.info("MidiIOManger available for tests"); }

    public void setInputDevice(int index) throws MidiUnavailableException {
        if (midiIn != null) {
            midiIn.close();
            logger.info("Closed previous input device and transmitter.");
        }
        MidiDevice device = MidiDeviceUtils.getDevice(index);
        logger.info("Opening input device: " + device.getDeviceInfo().getName());
        device.open();

        if (device.getMaxTransmitters() > 0 || device.getMaxTransmitters() == -1) {
            midiIn = new InputWrapper(device, server.getInputBuffer());
            if (sendEngine != null) midiIn.getInputReceiver().setIngressListener(sendEngine);
            inPort = index;
            logger.info("Transmitter set for input device index: " + index);
        } else {
            logger.warning("Device is output-only: " + index);
        }
    }

    public void setOutputDevice(int index) throws MidiUnavailableException {
        if (sendEngine != null) {
            sendEngine.stop();
            sendEngine = null;
        }

        MidiDevice device = MidiDeviceUtils.getDevice(index);
        midiOut = new ReceiverWrapper(device);
        logger.info("Opening output device: " + midiOut.getDeviceInfo().getName());
        outPort = index;

        // Start paced engine for the new device
        sendEngine = new MidiSendEngine(midiOut, 4096);
        sendEngine.setThroughputProfile(throughput);
        sendEngine.setTelemetryListener(WebSocketEndpoint::broadcast);
        sendEngine.start();
    }

    public void setMidiOutForTest(MidiOutput mock) {
        this.midiOut = mock;
        this.sendEngine = new MidiSendEngine(midiOut, 4096);
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

        if (!sendEngine.offer(data)) {
            logger.warning("Send queue full; message dropped or consider coalescing.");
        }
    }

    public boolean trySetOutputDevice(int index) {
        try {
            if (outPort != index) setOutputDevice(index);
            if (outPort != index) {
                logger.warning("Failed to set new output port to " + index + " the out port is " + outPort);
                return false;
            }
            logger.info("The current output device is: " + outPort);
            return true;
        } catch (MidiUnavailableException e) {
            logger.warning("Failed to open output device: " + e.getMessage());
            return false;
        }
    }

    public boolean trySetInputDevice(int index) {
        try {
            if (inPort != index) setInputDevice(index);
            if (inPort != index) {
                logger.warning("Failed to set new input port to " + index + " the in port is " + inPort);
                return false;
            }
            logger.info("The current input device is: " + inPort);
            return true;
        } catch (MidiUnavailableException e) {
            logger.warning("Failed to open input device: " + e.getMessage());
            return false;
        }
    }

    public TransportMode getTransportMode() {
        return (mode != null) ? mode : TransportMode.SYSEX;
    }
    public TransportMode setTransportMode(TransportMode changeTo) {
        return this.mode = changeTo;
    }

    public void setThroughputProfile(ThroughputProfile profile) {
        if (this.getThroughputProfile() == profile) return;
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
        return this.sendEngine.getThroughputProfile();
    }
}