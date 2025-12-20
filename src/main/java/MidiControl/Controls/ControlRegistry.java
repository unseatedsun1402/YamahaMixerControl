package MidiControl.Controls;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexRegistry;

public class ControlRegistry {
    private final Map<Integer, Control> controls = new HashMap<>();
    private final SysexRegistry sysexRegistry;

    public ControlRegistry(List<SysexMapping> mappings) {
        this.sysexRegistry = new SysexRegistry(mappings);

        // Group mappings by controlId
        Map<Integer, List<SysexMapping>> grouped = mappings.stream()
            .collect(Collectors.groupingBy(SysexMapping::getControl_id));

        // Create one Control per group
        for (Map.Entry<Integer, List<SysexMapping>> entry : grouped.entrySet()) {
            int controlId = entry.getKey();
            String groupName = entry.getValue().get(0).getControlGroup();
            String subControl = entry.getValue().get(0).getSubControl();

            Control control = new Control(controlId, groupName, subControl);
            // Attach all mappings that belong to this control group
            control.setMappings(entry.getValue());

            controls.put(controlId, control);
        }
    }

    /**
     * Resolve a SysEx message to its Control group.
     * Uses SysexRegistry to find the mapping, then returns the Control for that mapping’s controlId.
     */
    public Control resolveSysex(byte[] message) {
        SysexMapping mapping = sysexRegistry.resolve(message);
        if (mapping == null) return null;
        return controls.get(mapping.getControl_id());
    }

    public Control getControl(int controlId) {
        return controls.get(controlId);
    }
}

