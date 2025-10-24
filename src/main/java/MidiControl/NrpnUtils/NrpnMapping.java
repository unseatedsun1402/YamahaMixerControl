package MidiControl.NrpnUtils;
import MidiControl.ChannelMappings.ChannelType;
import MidiControl.ChannelMappings.ControlType;

public record NrpnMapping(
    int msb,
    int lsb,
    ControlType controlType,
    ChannelType channelType,
    int channelIndex,
    String label
) {}
