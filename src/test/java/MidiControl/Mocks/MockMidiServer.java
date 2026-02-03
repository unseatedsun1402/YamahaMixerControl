package MidiControl.Mocks;

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.sound.midi.MidiMessage;

import MidiControl.ControlServer.CanonicalInputEvent;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;
import MidiControl.MidiDeviceManager.MidiIOManager;
import MidiControl.Server.MidiServer;
import MidiControl.Server.RehydrationManager;
import MidiControl.Server.ServerRouter;
import MidiControl.SysexUtils.SysexParser;

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

    @Override
    public void processIncomingMidiForTest() {
        ConcurrentLinkedQueue<MidiMessage> buffer = this.getInputBuffer();
        // Drain all incoming messages
        while (!buffer.isEmpty()) {

            var message = buffer.poll();
            if (message == null) continue;

            try {
                ControlInstance instance = registry.resolveSysex(message.getMessage());
                if (instance != null) {
                    System.out.println("Resolved "+instance.getCanonicalId());
                    CanonicalInputEvent event = new CanonicalInputEvent(CanonicalInputEvent.Type.SYSEX,message.getMessage(),null,null);
                    int value = instance.extractValue(event);
                    System.out.println("Updating "+instance.getCanonicalId()+ "  to "+(char)value);
                    instance.updateValue(value);
                }
                else{System.out.println("Could not resolve message"+SysexParser.bytesToHex(message.getMessage()));}
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

}