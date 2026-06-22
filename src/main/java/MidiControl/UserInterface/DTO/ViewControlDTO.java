package MidiControl.UserInterface.DTO;

public class ViewControlDTO {

    public String logicId;
    public String uiGroup;
    public String label;
    public String type;
    public int index;

    public String hwGroup;
    public String hwSubcontrol;
    public int hwInstance;
    public String canonicalId;

    public int value;
    public int min;
    public int max;

    public boolean bipolar;
    public boolean stepped;
    public boolean readOnly;
    public String unit;

    public String controlRole;
    public Integer sendIndex;
    public Integer channelIndex;
    public String viewType;
    public String viewSuffix;
}
