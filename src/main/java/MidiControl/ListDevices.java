package MidiControl;

import javax.sound.midi.*;

public class ListDevices {
    public static void listDevices() throws MidiUnavailableException {
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (MidiDevice.Info info : infos) {
            System.out.println("Name: " + info.getName() +", accepts Input: " + MidiSystem.getMidiDevice(info).getMaxReceivers() + ", accepts Output: " + MidiSystem.getMidiDevice(info).getMaxTransmitters() + ", Index: " + java.util.Arrays.asList(infos).indexOf(info));
        }
    }
}
