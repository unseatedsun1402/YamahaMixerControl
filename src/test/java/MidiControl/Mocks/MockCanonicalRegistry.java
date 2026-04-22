
package MidiControl.Mocks;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.ControlInstance;
import MidiControl.Controls.SubControl;

import java.util.*;

public class MockCanonicalRegistry extends CanonicalRegistry {

    private final Map<String, List<ControlInstance>> contextMap = new HashMap<>();
    private final Map<String, ControlInstance> canonicalMap = new HashMap<>();

    public MockCanonicalRegistry() {
        super(Collections.emptyList(), null); // SAFE: empty mappings list
    }

    public void mapContext(String contextId, ControlGroup... groups) {
        List<ControlInstance> list = new ArrayList<>();
        for (ControlGroup g : groups) {
            for (SubControl sc : g.getSubcontrols().values()) {
                for (ControlInstance ci : sc.getInstances()) {
                    list.add(ci);
                    // Auto-index by canonical id if available
                    String id = ci.getCanonicalId();
                    if (id != null) canonicalMap.put(id, ci);
                }
            }
        }
        contextMap.put(contextId, list);
    }

    public void registerCanonical(String canonicalId, ControlInstance instance) {
        canonicalMap.put(canonicalId, instance);
    }

    @Override
    public List<ControlInstance> getAllInstancesForContext(String contextId) {
        return contextMap.getOrDefault(contextId, List.of());
    }

    @Override
    public ControlInstance resolveCanonicalId(String canonicalId) {
        return canonicalMap.get(canonicalId);
    }
}
