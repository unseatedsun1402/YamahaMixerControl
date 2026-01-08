package MidiControl.Mocks;

import javax.sound.midi.MidiDevice;

import MidiControl.Services.MidiDeviceProvider;

public class MockMidiDeviceProvider implements MidiDeviceProvider {

    private final MidiDevice.Info[] infos;
    private final MidiDevice[] devices;

    public MockMidiDeviceProvider(MidiDevice.Info[] infos, MidiDevice[] devices) {
        this.infos = infos;
        this.devices = devices;
    }

    @Override
    public MidiDevice.Info[] getDeviceInfos() {
        return infos;
    }

    @Override
    public MidiDevice getDevice(MidiDevice.Info info) {
        for (int i = 0; i < infos.length; i++) {
            if (infos[i] == info) return devices[i];
        }
        throw new IllegalArgumentException("Unknown device: " + info);
    }
}