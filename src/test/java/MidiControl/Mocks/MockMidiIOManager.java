package MidiControl.Mocks;

import MidiControl.MidiDeviceManager.MidiDeviceDTO;
import MidiControl.MidiDeviceManager.MidiInput;
import MidiControl.MidiDeviceManager.MidiOutput;
import MidiControl.MidiDeviceManager.MidiIOManager;
import MidiControl.Server.MidiServer;

import java.util.ArrayList;
import java.util.List;

public class MockMidiIOManager extends MidiIOManager {

    public MockMidiIOManager(MidiServer server) {
        super(server);
    }

    // --- State used by tests ---
    private MidiOutput out;
    private MidiInput in;

    public List<MidiDeviceDTO> devices = new ArrayList<>();

    public boolean setResult = true;   // result returned by trySetOutputDevice
    public int lastSetIndex = -1;      // track which device index was requested


    @Override
    public MidiOutput getMidiOut() {
        return out;
    }

    @Override
    public MidiInput getMidiIn() {
        return in;
    }

    @Override
    public boolean trySetOutputDevice(int index) {
        this.lastSetIndex = index;
        return setResult;
    }

    @Override
    public boolean trySetInputDevice(int index) {
        return setResult;
    }

    @Override
    public List<MidiDeviceDTO> listDeviceDTOs() {
        return devices;
    }

    public void setMidiOutForTest(MidiOutput out) {
        this.out = out;
    }

    public void setMidiInForTest(MidiInput in) {
        this.in = in;
    }

    @Override
    public void sendAsync(byte[] message){
        this.out.sendMessage(message);
    }

    /**
     * Copy state from another MockMidiIoManager.
     * Used by the rewritten ServerRouterTest helper.
     */
    public void copyFrom(MockMidiIOManager other) {
        this.out = other.out;
        this.in = other.in;
        this.devices = new ArrayList<>(other.devices);
        this.setResult = other.setResult;
        this.lastSetIndex = other.lastSetIndex;
    }
}