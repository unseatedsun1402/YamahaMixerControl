package MidiControl.Mocks;

import javax.sound.midi.*;
import java.util.List;

public class MockMidiDevice implements MidiDevice {

    private final Info info;
    private final int maxTransmitters;
    private final int maxReceivers;

    public MockMidiDevice(Info info, int maxTransmitters, int maxReceivers) {
        this.info = info;
        this.maxTransmitters = maxTransmitters;
        this.maxReceivers = maxReceivers;
    }

    @Override public Info getDeviceInfo() { return info; }
    @Override public int getMaxTransmitters() { return maxTransmitters; }
    @Override public int getMaxReceivers() { return maxReceivers; }

    @Override public void open() {}
    @Override public void close() {}
    @Override public boolean isOpen() { return true; }
    @Override public long getMicrosecondPosition() { return 0; }
    @Override public Receiver getReceiver() { return null; }
    @Override public List<Receiver> getReceivers() { return List.of(); }
    @Override public Transmitter getTransmitter() { return null; }
    @Override public List<Transmitter> getTransmitters() { return List.of(); }
}