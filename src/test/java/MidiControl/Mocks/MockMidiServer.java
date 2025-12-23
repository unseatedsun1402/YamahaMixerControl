package MidiControl.Mocks;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.MidiDeviceManager.MidiIOManager;
import MidiControl.Server.MidiServer;

public class MockMidiServer extends MidiServer {

    private final CanonicalRegistry registry;
    private MockMidiIOManager io;

    public String lastSent;
    public int lastValue;

    public MockMidiServer(CanonicalRegistry registry) {
        super(); // If MidiServer requires args, adjust accordingly
        this.registry = registry;
        this.io = new MockMidiIOManager(this);
    }

    @Override
    public CanonicalRegistry getCanonicalRegistry() {
        return registry;
    }

    @Override
    public MidiIOManager getMidiDeviceManager() {
        return io;
    }

    public MockMidiIOManager getMockIo() {
        return io;
    }

    public void setMockIo(MockMidiIOManager io) {
        this.io = io;
    }

    // Called by ServerRouter when handling set-control-value
    public void recordGuiChange(String canonicalId, int value) {
        this.lastSent = canonicalId;
        this.lastValue = value;
    }
}