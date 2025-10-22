package MidiControl.SysexUtils;

import java.util.List;

public class SysExControl {
    public String controlId;
    public int channelIndex;
    public List<Integer> parameterChangeFormat;
    public List<Integer> parameterRequestFormat;
    public Integer defaultValue;
    public String name;
    public String comment;
    public Integer minValue;
    public Integer maxValue;
}