package MidiControl.Mocks;

import MidiControl.MidiDeviceManager.MidiOutput;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;

import java.util.ArrayList;
import java.util.List;

public class MockMidiOut implements MidiOutput {

    public final List<byte[]> sent = new ArrayList<>();
    private Boolean openFlag = true;

    @Override
    public void sendMessage(byte[] message) {
        sent.add(message);
    }

    @Override
    public MidiDevice.Info getDeviceInfo() {
        return null;
    }

    @Override
    public boolean isOpen() {
        return openFlag;
    }

    @Override
    public void close() {
        openFlag = false;
    }

    @Override
    public void sendMessage(MidiMessage message) {
        sent.add(message.getMessage());
    }

    public List<byte[]> getSentMessages() {
        return this.sent;
    }
}