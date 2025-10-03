package MidiControl.Utilities;
import MidiControl.ChannelMappings.*;

public record SysExMapping(
    String elementId,
    byte[] addressBytes,
    int valueLength,
    String comment,
    boolean txSupported,
    boolean rxSupported,
    ChannelType sourceChannelType,
    int sourceChannelIndex,
    ChannelType targetChannelType,
    int targetChannelIndex,
    ControlType controlType
) {}
