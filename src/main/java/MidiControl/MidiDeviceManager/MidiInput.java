package MidiControl.MidiDeviceManager;

import javax.sound.midi.Receiver;

public interface MidiInput {
    void setReceiver(Receiver receiver);
    void close();
}
