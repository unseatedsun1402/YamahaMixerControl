package MidiControl.Services;

import javax.sound.midi.MidiDevice;

public interface MidiDeviceProvider {
    MidiDevice.Info[] getDeviceInfos();
    MidiDevice getDevice(MidiDevice.Info info) throws Exception;
}
