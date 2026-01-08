package MidiControl.Controls;

import java.util.*;

public class ControlGroup {
    private final String name;
    private final Map<String, SubControl> subcontrols = new HashMap<>();

    public ControlGroup(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addSubcontrol(SubControl sub) {
        subcontrols.put(sub.getName(), sub);
    }

    public SubControl getSubcontrol(String name) {
        return subcontrols.get(name);
    }

    public Map<String, SubControl> getSubcontrols() {
        return subcontrols;
    }
}