package MidiControl.Mocks;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.ControlInstance;
import MidiControl.Controls.SubControl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockCanonicalRegistry extends CanonicalRegistry {

    private final Map<String, List<ControlInstance>> contextMap = new HashMap<>();

    public MockCanonicalRegistry() {
        super(Collections.emptyList(), null); // SAFE: empty mappings list
    }

    public void mapContext(String contextId, ControlGroup... groups) {
        List<ControlInstance> list = new ArrayList<>();
        for (ControlGroup g : groups) {
            for (SubControl sc : g.getSubcontrols().values()) {
                list.addAll(sc.getInstances());
            }
        }
        contextMap.put(contextId, list);
    }

    @Override
    public List<ControlInstance> getAllInstancesForContext(String contextId) {
        return contextMap.getOrDefault(contextId, List.of());
    }
}
