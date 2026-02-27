package MidiControl.Mocks;

import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.sound.midi.MidiMessage;

import MidiControl.ControlServer.CanonicalInputEvent;
import MidiControl.ControlServer.HardwareInputHandler;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;
import MidiControl.MidiDeviceManager.MidiIOManager;
import MidiControl.NrpnUtils.NrpnParser;
import MidiControl.NrpnUtils.NrpnRegistry;
import MidiControl.Server.MidiServer;
import MidiControl.Server.RehydrationManager;
import MidiControl.Server.ServerRouter;
import MidiControl.SysexUtils.SysexParser;
import jakarta.annotation.Nullable;

public class MockMidiServer extends MidiServer {

    private final CanonicalRegistry registry;
    private MockMidiIOManager io;

    public String lastSent;
    public int lastValue;
    private RehydrationManager rehydrationManager;
    private ServerRouter serverRouter;
    private static NrpnParser nrpnParser;
    private static NrpnRegistry nrpnRegistry;

    public MockMidiServer(CanonicalRegistry registry) {
        super(registry); // ← use the test constructor, NOT the default one
        this.registry = registry;
        this.io = new MockMidiIOManager(this);

        // If you want rehydration in tests:
        this.rehydrationManager = new RehydrationManager();
    }

    public static void setNrpnFields(NrpnParser parser, NrpnRegistry reg){
        nrpnParser = parser;
        nrpnRegistry = reg;
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
        while (!buffer.isEmpty()) {
            var message = buffer.poll();
            if (message == null) continue;

            try {
                CanonicalInputEvent event = new HardwareInputHandler(nrpnParser, nrpnRegistry).handle(message);
                ControlInstance instance = registry.resolve(event);
                if (instance != null) {
                    System.out.println("Resolved "+instance.getCanonicalId());
                    if(event.getCc()!=null) System.out.println("event control chg content: "+event.getCc());
                    if(event.getNrpn() != null) System.out.println("event nrpn content: "+event.getNrpn().msb+","+
                        event.getNrpn().lsb+
                        ","+event.getNrpn().value);

                    if(event.getSysexData() != null) System.out.println("event sysex content: "+event.getSysexData().toString());
                    int value = instance.extractValue(event);
                    System.out.println("Updating "+instance.getCanonicalId()+ "  to "+value+" | "+(char)value);
                    instance.updateValue(value);
                }
                else{System.out.println("Could not resolve message "+SysexParser.bytesToHex(message.getMessage()));}
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

}