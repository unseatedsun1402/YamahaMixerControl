package MidiControl.MidiDeviceManager;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.ShortMessage;

public interface MidiOutput {
    void sendMessage(ShortMessage message);
    MidiDevice.Info getDeviceInfo();
}

