package MidiControl.Mocks;

import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.ShortMessage;

import MidiControl.MidiDeviceManager.MidiOutput;

public class MockMidiOutput implements MidiOutput {
    private final List<ShortMessage> sentMessages = new ArrayList<>();

    @Override
    public void sendMessage(ShortMessage message) {
        sentMessages.add(message);
    }

    public List<ShortMessage> getSentMessages() {
        return sentMessages;
    }

    @Override
    public Info getDeviceInfo() {
        return MidiSystem.getMidiDeviceInfo()[0]; // 0 should be the system internal midi interface
    }
}
