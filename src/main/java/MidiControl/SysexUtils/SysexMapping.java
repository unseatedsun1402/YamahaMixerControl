package MidiControl.SysexUtils;

import java.util.List;

public class SysexMapping {
    private String control_group;
    private String control_id;
    private String sub_control;
    private int channel_index;
    private int value;
    private int min_value;
    private int max_value;
    private int default_value;
    private String comment;
    private List<Object> parameter_change_format;
    private List<Object> parameter_request_format;

    // Getters and setters
    public String getControlGroup() { return control_group; }
    public String getControl_id() { return control_id; }
    public String getSubControl() { return sub_control; }
    public int getChannel_index() { return channel_index; }
    public int getValue() { return value; }
    public int getMin_value() { return min_value; }
    public int getMax_value() { return max_value; }
    public int getDefault_value() { return default_value; }
    public String getComment() { return comment; }
    public List<Object> getParameter_change_format() { return parameter_change_format; }
    public List<Object> getParameter_request_format() { return parameter_request_format; }
}