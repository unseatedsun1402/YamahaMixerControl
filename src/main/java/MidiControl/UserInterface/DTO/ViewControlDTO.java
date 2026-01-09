package MidiControl.UserInterface.DTO;

public class ViewControlDTO {

    public String logicId;     // e.g. "PAN", "CHANNEL_ON", "SEND_MIX1"
    public String uiGroup;     // e.g. "kInputPan", "kInputToMix"
    public String label;       // e.g. "Pan", "On", "Mix 1"
    public String type;        // e.g. "SLIDER_HORIZONTAL", "KNOB", "FADER"
    public int index;          // ordering within uiGroup

    public String hwGroup;       // e.g. "kInputPan"
    public String hwSubcontrol;  // e.g. "kChannelPan"
    public int hwInstance;       // e.g. 1
    public String canonicalId;

    public double value;
    public double min;
    public double max;

    public boolean bipolar;
    public boolean stepped;
    public boolean readOnly;
    public String unit;
}