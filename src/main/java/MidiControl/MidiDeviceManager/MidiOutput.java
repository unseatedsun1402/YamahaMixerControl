package MidiControl.MidiDeviceManager;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;

public interface MidiOutput {

    // New primary API: send raw bytes
    void sendMessage(byte[] message);

    // Optional: legacy API (still useful for debugging or direct sends)
    void sendMessage(MidiMessage message);

    MidiDevice.Info getDeviceInfo();

    boolean isOpen();

    void close();
}