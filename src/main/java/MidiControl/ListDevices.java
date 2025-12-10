package MidiControl;

import javax.sound.midi.*;


public class ListDevices {
    static void main(String[] args) {
        listDevices();
    }

    
    static void listDevices() {
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (MidiDevice.Info info : infos) {
            try {
                System.out.println("Name: " + info.getName() +", accepts Input: " + MidiSystem.getMidiDevice(info).getMaxReceivers() + ", accepts Output: " + MidiSystem.getMidiDevice(info).getMaxTransmitters() + ", Index: " + java.util.Arrays.asList(infos).indexOf(info));
            } catch (MidiUnavailableException e) {
                e.printStackTrace();
            }
        }
    }
}
