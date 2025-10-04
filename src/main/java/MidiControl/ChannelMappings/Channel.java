package MidiControl.ChannelMappings;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

public class Channel {
    private final ChannelType type;
    private final int index; // e.g., 0 for INPUT 01

    public Channel(ChannelType type, int index) {
        this.type = type;
        this.index = index;
    }

    public ShortMessage createMidiMessage(MidiControl.ChannelMappings.ControlType control, int value) throws InvalidMidiDataException {
        return new ShortMessage(0xB0, control.getControlByte(), value);
    }

    public String getLabel() {
        return type.name() + " " + (index + 1);
    }

    // Getters
    public ChannelType getType() { return type; }
    public int getIndex() { return index; }
}

