package MidiControl.Controls;

import java.util.*;

public class SubControl {
    private final String name;
    private final List<ControlInstance> instances = new ArrayList<>();
    private ControlGroup parent_group;

    public SubControl(ControlGroup parent, String name) {
        this.name = name;
        this.parent_group = parent;
    }

    public String getName() {
        return name;
    }

    public ControlGroup getParentGroup(){
        return this.parent_group;
    }

    public void addInstance(ControlInstance instance) {
        instances.add(instance);
    }

    public List<ControlInstance> getInstances() {
        return instances;
    }
}