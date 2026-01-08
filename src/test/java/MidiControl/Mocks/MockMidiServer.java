package MidiControl.Mocks;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.MidiDeviceManager.MidiIOManager;
import MidiControl.Server.MidiServer;
import MidiControl.Server.RehydrationManager;
import MidiControl.Server.ServerRouter;

public class MockMidiServer extends MidiServer {

    private final CanonicalRegistry registry;
    private MockMidiIOManager io;

    public String lastSent;
    public int lastValue;
    private RehydrationManager rehydrationManager;
    private ServerRouter serverRouter;

    public MockMidiServer(CanonicalRegistry registry) {
        super(registry); // ← use the test constructor, NOT the default one
        this.registry = registry;
        this.io = new MockMidiIOManager(this);

        // If you want rehydration in tests:
        this.rehydrationManager = new RehydrationManager();
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

    public void recordGuiChange(String canonicalId, int value) {
        this.lastSent = canonicalId;
        this.lastValue = value;
    }
}