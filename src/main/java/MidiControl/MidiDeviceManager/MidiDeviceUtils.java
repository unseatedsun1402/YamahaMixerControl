package MidiControl.MidiDeviceManager;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;

public final class MidiDeviceUtils {

    private MidiDeviceUtils() {} // prevent instantiation

    public static MidiDevice getDevice(int index) throws MidiUnavailableException {
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        if (index < 0 || index >= infos.length) {
            throw new MidiUnavailableException("Invalid device index: " + index);
        }
        return MidiSystem.getMidiDevice(infos[index]);
    }
}