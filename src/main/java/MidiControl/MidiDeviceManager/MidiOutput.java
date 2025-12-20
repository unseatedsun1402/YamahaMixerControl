package MidiControl.MidiDeviceManager;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;

public interface MidiOutput {
    void sendMessage(MidiMessage message);
    MidiDevice.Info getDeviceInfo();
    boolean isOpen();
    void close();
}

