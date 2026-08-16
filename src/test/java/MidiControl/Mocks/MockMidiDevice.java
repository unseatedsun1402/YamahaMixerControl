package MidiControl.Mocks;

import javax.sound.midi.*;

import MidiControl.MidiDeviceManager.MidiOutput;

import java.util.List;

public class MockMidiDevice implements MidiDevice, MidiOutput {

    private final Info info;
    private final int maxTransmitters;
    private final int maxReceivers;

    private final Receiver receiver;
    private final Transmitter transmitter;

    public MockMidiDevice(Info info, int maxTransmitters, int maxReceivers, Receiver customReceiver) {
        this.info = info;
        this.maxTransmitters = maxTransmitters;
        this.maxReceivers = maxReceivers;

        this.receiver = customReceiver;
        this.transmitter = new MockTransmitter(customReceiver);
    }

    public MockMidiDevice(Info info, int maxTransmitters, int maxReceivers) {
        this(info, maxTransmitters, maxReceivers, new MockReceiver());
    }

    @Override public Receiver getReceiver() { return receiver; }
    @Override public List<Receiver> getReceivers() { return List.of(receiver); }
    @Override public Transmitter getTransmitter() { return transmitter; }
    @Override public List<Transmitter> getTransmitters() { return List.of(transmitter); }

    @Override
    public void sendMessage(byte[] message) {
        try {
            SysexMessage sysex = new SysexMessage();
            sysex.setMessage(message, message.length);
            receiver.send(sysex, -1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendMessage(MidiMessage message) {
        receiver.send(message, -1);
    }

    @Override public Info getDeviceInfo() { return info; }
    @Override public int getMaxTransmitters() { return maxTransmitters; }
    @Override public int getMaxReceivers() { return maxReceivers; }

    @Override public void open() {}
    @Override public void close() {}
    @Override public boolean isOpen() { return true; }
    @Override public long getMicrosecondPosition() { return 0; }
}
