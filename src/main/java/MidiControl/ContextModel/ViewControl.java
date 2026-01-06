package MidiControl.ContextModel;

import MidiControl.Controls.ControlInstance;
import java.util.List;

/**
 * Canonical UI ViewControl
 *
 * Represents a single UI control in a channel strip.
 * Contains:
 *  - logic_id      (UI identity, e.g. "PAN", "SEND_MIX1")
 *  - uiGroup       (UI grouping, e.g. "kInputPan")
 *  - label         (UI label)
 *  - type          (UI control type)
 *  - index         (ordering within group)
 *
 *  - canonical_id  (hardware identity: group, subcontrol, instance)
 *  - min/max/value (raw hardware values)
 *
 *  - optional UI hints (bipolar, stepped, readOnly, unit)
 *  - optional composite controls (multi)
 */
public class ViewControl {

    // ---------------------------------------------------------------------
    // UI Identity (canonical UI model)
    // ---------------------------------------------------------------------
    public String logicId;     // e.g. "PAN", "CHANNEL_ON", "SEND_MIX1"
    public String uiGroup;     // e.g. "kInputPan", "kInputToMix"
    public String label;       // e.g. "Pan", "On", "Mix 1"
    public ControlType type;   // KNOB, FADER, TOGGLE, SLIDER_HORIZONTAL
    public int index;          // ordering within uiGroup

    // ---------------------------------------------------------------------
    // Hardware Identity (canonical_id)
    // ---------------------------------------------------------------------
    public String hwGroup;       // e.g. "kInputPan"
    public String hwSubcontrol;  // e.g. "kChannelPan"
    public int hwInstance;       // e.g. 1 (channel index)

    // ---------------------------------------------------------------------
    // Hardware Value Range
    // ---------------------------------------------------------------------
    public int min;
    public int max;
    public int value;
    public int defaultValue;

    // ---------------------------------------------------------------------
    // Optional UI hints
    // ---------------------------------------------------------------------
    public boolean bipolar = false;
    public boolean stepped = false;
    public boolean readOnly = false;
    public String unit = null;

    // ---------------------------------------------------------------------
    // Optional composite controls (e.g. channel name)
    // ---------------------------------------------------------------------
    public List<ControlInstance> multi = null;

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------
    public String getCanonicalId() {
        return hwGroup + "." + hwSubcontrol + "." + hwInstance;
    }

    public String getLogicalId(){
        return this.logicId;
    }

    // ---------------------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------------------
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
            int hwInstance
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
    }
}