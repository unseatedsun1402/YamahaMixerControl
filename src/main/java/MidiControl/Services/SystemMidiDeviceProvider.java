package MidiControl.Services;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;

public class SystemMidiDeviceProvider implements MidiDeviceProvider {

    @Override
    public MidiDevice.Info[] getDeviceInfos() {
        return MidiSystem.getMidiDeviceInfo();
    }

    @Override
    public MidiDevice getDevice(MidiDevice.Info info) throws Exception {
        return MidiSystem.getMidiDevice(info);
    }
}