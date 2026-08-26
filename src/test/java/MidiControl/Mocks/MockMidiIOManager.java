package MidiControl.Mocks;

import MidiControl.MidiDeviceManager.MidiDeviceDTO;
import MidiControl.MidiDeviceManager.MidiInput;
import MidiControl.MidiDeviceManager.MidiOutput;
import MidiControl.MidiDeviceManager.MidiSendEngine;
import MidiControl.MidiDeviceManager.MidiSendEngine.ThroughputProfile;
import MidiControl.MidiDeviceManager.MidiIOManager;
import MidiControl.Server.MidiServer;
import MidiControl.Server.Rehydration.RehydrationManager;

import java.util.ArrayList;
import java.util.List;

public class MockMidiIOManager extends MidiIOManager {

    public MockMidiIOManager(MidiServer server) {
        super(server);
    }

    // --- State used by tests ---
    private MidiOutput out;
    private MidiInput in;
    public MidiSendEngine sendEngine;

    public List<MidiDeviceDTO> devices = new ArrayList<>();

    public boolean setResult = true;   // result returned by trySetOutputDevice
    public int lastSetIndex = -1;      // track which device index was requested
    public byte[] lastSentMessage;
    public List<byte[]> sentMessages = new ArrayList<byte[]>();
    private RehydrationManager rehydrationManager;

    public int lastOutIndex = -1;
    public int lastInIndex = -1;
    public boolean simulateHasDevices = true;

    @Override
    public RehydrationManager getRehydrationManager() {
        return rehydrationManager != null ? rehydrationManager : new MockRehydrationManager();
    }

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
    public boolean hasValidDevices(){
        if(simulateHasDevices)return true;
        
        if (lastOutIndex < 0 || lastInIndex < 0) return false;
        if (lastOutIndex >= devices.size()) return false;
        if (lastInIndex >= devices.size()) return false;

        MidiDeviceDTO out = devices.get(lastOutIndex);
        MidiDeviceDTO in = devices.get(lastInIndex);

        return out.canOutput && in.canInput;
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
        this.lastSentMessage = message;
        this.sentMessages.add(message);
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

    @Override
    public ThroughputProfile getThroughputProfile() {
        return MidiSendEngine.ThroughputProfile.SAFE_DIN;
    }

    @Override
    public  void setThroughputProfile(MidiSendEngine.ThroughputProfile profile){
        return;
    }

    public void setRehydrationManager(RehydrationManager manager){
        this.rehydrationManager = manager;
    }
}
