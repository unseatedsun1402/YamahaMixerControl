package MidiControl;

import javax.sound.midi.ShortMessage;

import java.util.Map;

import javax.sound.midi.MidiMessage;

import MidiControl.ChannelMappings.ControlType;
public class ParseMidi {
    /**
     * Parses a ShortMessage into its components
     * @param message
     * @return int[]
     */
    public static String resolveControlType(ShortMessage message){
        int data1 = message.getData1();
        for (ControlType type : ControlType.values()) {
            if (type.getControlByte() == data1) {
                return type.name();
            }
        }
        return null;
    }

    public static void printResolvedTypes(ShortMessage message){
        String controlType = resolveControlType(message);
        if (controlType != null) {
            System.out.println("Control Type: " + controlType + ", Value: " + message.getData2());
        } else {
            System.out.println("Unknown Control Type for data1: " + message.getData1());
        }
    }

}
