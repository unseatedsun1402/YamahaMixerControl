package MidiControl.ContextModel;

public class ContextFilter {
    private final String controlGroup;   // e.g. "kInput"
    private final String subControl;     // e.g. "Fader" or "*"
    private final Integer index;         // null = all

    public ContextFilter(String controlGroup, String subControl, Integer index) {
        this.controlGroup = controlGroup;
        this.subControl = subControl;
        this.index = index;
    }

    public String getControlGroup() { return controlGroup; }
    public String getSubControl() { return subControl; }
    public Integer getIndex() { return index; }
}
