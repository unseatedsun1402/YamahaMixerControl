package MidiControl.MidiDeviceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;

import MidiControl.Server.MidiServer;

public class MidiIOManager {

    private MidiOutput midiOut;
    private MidiInput  midiIn;
    private int        outPort;
    private int         inPort;
    private MidiServer server;
    private final Logger logger = Logger.getLogger(MidiIOManager.class.getName());
    private TransportMode mode;

    public MidiIOManager(MidiServer server) {
        this.server = server;
    }

    public void setInputDevice(int index) throws MidiUnavailableException {
        if (midiIn != null) {
            midiIn.close();
            logger.info("Closed previous input device and transmitter.");
        }

        MidiDevice device = MidiDeviceUtils.getDevice(index);
        logger.info("Opening input device: " + device.getDeviceInfo().getName());
        device.open();

        logger.info("After open -> Transmitters: " + device.getMaxTransmitters());

        if (device.getMaxTransmitters() > 0 || device.getMaxTransmitters() == -1) {
            midiIn = new TransmitterWrapper(device, server.getInputBuffer());
            inPort = index;
            logger.info("Transmitter set for input device index: " + index);
            MidiDevice.Info info = midiIn.getDeviceInfo();
            // Settings.updateInputDevice(index, info.getName(), info.getDescription());
        } else {
            logger.warning("Device is output-only: " + index);
        }
    }

    public void setOutputDevice(int index) throws MidiUnavailableException {
        MidiDevice device = MidiDeviceUtils.getDevice(index);
        midiOut = new ReceiverWrapper(device);

        MidiDevice.Info info = midiOut.getDeviceInfo();
        logger.info("Opening output device: " + info.getName());
        outPort = index;
        // Settings.updateOutputDevice(index, info.getName(), info.getDescription());
    }

    // Test-only helper
    public void setMidiOutForTest(MidiOutput mock) {
        this.midiOut = mock;
    }

    public void shutdown() {
        logger.info("Shutting down MIDI devices.");
        if (midiIn != null && midiIn.isOpen()) {
            midiIn.close();
        }
        if (midiOut != null && midiOut.isOpen()) {
            midiOut.close();
        }
    }

    public MidiOutput getMidiOut() { return midiOut; }
    public MidiInput getMidiIn() { return midiIn; }

    private List<MidiDevice.Info> listDevices() {
        return Arrays.asList(MidiSystem.getMidiDeviceInfo());
    }

    public List<MidiDeviceDTO> listDeviceDTOs() {
        List <MidiDevice.Info> infos = listDevices();
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

    public boolean trySetOutputDevice(int index) {
        try {
            if(outPort != index){setOutputDevice(index);}
            if(outPort != index){logger.warning("Failed to set new output port to" + index + 
            " the out port is "+ outPort); return false;}
            logger.info("The current output device is: "+ outPort);
            return true;
        } catch (MidiUnavailableException e) {
            logger.warning("Failed to open output device: " + e.getMessage());
            return false;
        }
    }

    public boolean trySetInputDevice(int index) {
        try {
            if(inPort != index){setInputDevice(index);}
            if(inPort != index){logger.warning("Failed to set new input port to " + index
             + " the in port is "+ outPort); return false;}
            logger.info("The current input device is: "+inPort);
            return true;
        } catch (MidiUnavailableException e) {
            logger.warning("Failed to open input device: " + e.getMessage());
            return false;
        }
    }

    public TransportMode getTransportMode(){
        if(mode != null){return mode;}
        return TransportMode.SYSEX;
    }

    public TransportMode setTransportMode(TransportMode changeTo){
        return this.mode = changeTo;
    }
}