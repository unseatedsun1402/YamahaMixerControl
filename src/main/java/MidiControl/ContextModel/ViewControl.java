package MidiControl.ContextModel;

import java.util.List;

import MidiControl.Controls.ControlInstance;

public class ViewControl {

    // --- UI ---
    public String logicId;
    public String uiGroup;
    public String label;
    public ControlType type;
    public int index;

    // --- Semantic (lightweight) ---
    public String controlRole;    // e.g. INPUT_SEND_LEVEL
    public Integer sendIndex;     // e.g. 4
    public Integer channelIndex;  // e.g. 4

    // --- View context ---
    public String viewType;       // e.g. basic-input-view
    public String viewSuffix;     // e.g. mix4

    // --- Hardware ---
    public String canonicalId;
    public String hwGroup;
    public String hwSubcontrol;
    public int hwInstance;

    public int min;
    public int max;
    public int value;
    public int defaultValue;

    public boolean bipolar = false;
    public boolean stepped = false;
    public boolean readOnly = false;
    public String unit = null;

    public List<ControlInstance> multi = null;

    public ViewControl(
            String logicId,
            String uiGroup,
            String label,
            ControlType type,
            int index,
            int min,
            int max,
            int value,
            int defaultValue,
            String hwGroup,
            String hwSubcontrol,
            int hwInstance,
            String viewType,
            String viewSuffix,
            String controlRole,
            Integer sendIndex,
            Integer channelIndex
    ) {
        this.logicId = logicId;
        this.uiGroup = uiGroup;
        this.label = label;
        this.type = type;
        this.index = index;

        this.min = min;
        this.max = max;
        this.value = value;
        this.defaultValue = defaultValue;

        this.hwGroup = hwGroup;
        this.hwSubcontrol = hwSubcontrol;
        this.hwInstance = hwInstance;

        this.canonicalId = hwGroup + "." + hwSubcontrol + "." + hwInstance;

        this.viewType = viewType;
        this.viewSuffix = viewSuffix;

        this.controlRole = controlRole;
        this.sendIndex = sendIndex;
        this.channelIndex = channelIndex;
    }
}