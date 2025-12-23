package MidiControl.Controls;

import MidiControl.SysexUtils.SysexMapping;
import java.util.*;


public class ControlFactory {

    public static Map<String, ControlGroup> fromSysexMappings(List<SysexMapping> mappings) {

        Map<String, ControlGroup> groups = new HashMap<>();

        for (SysexMapping mapping : mappings) {

            String groupName = mapping.getControlGroup();   // e.g. "kMixFader"
            String subName   = mapping.getSubControl();     // e.g. "kFader"

            // 1. Get or create ControlGroup
            ControlGroup group = groups.computeIfAbsent(groupName, ControlGroup::new);

            // 2. Get or create Subcontrol
            SubControl sub = group.getSubcontrol(subName);
            if (sub == null) {
                sub = new SubControl(group, subName);
                group.addSubcontrol(sub);
            }

            for (int instance=0; instance < mapping.getMax_Channels(); instance ++) {
              ControlInstance control = new ControlInstance(
                sub, instance,
                mapping,   // store the entire SysexMapping object
                null // NRPN mapping added later
              );
              // 4. Add instance to subcontrol
              sub.addInstance(control);
            }
        }
        return groups;
    }
}
