package MidiControl.Controls;

import MidiControl.SysexUtils.SysexMapping;
import java.util.*;


public class ControlFactory {

    public static Map<String, ControlGroup> fromSysexMappings(List<SysexMapping> mappings) {

        Map<String, ControlGroup> groups = new HashMap<>();

        for (SysexMapping mapping : mappings) {

            String groupName = mapping.getControlGroup();
            String subName   = mapping.getSubControl();

            ControlGroup group = groups.computeIfAbsent(groupName, ControlGroup::new);

            SubControl sub = group.getSubcontrol(subName);
            if (sub == null) {
                sub = new SubControl(group, subName);
                group.addSubcontrol(sub);
            }

            for (int instance=0; instance < mapping.getMax_Channels(); instance ++) {
              ControlInstance control = new ControlInstance(
                sub, instance,
                mapping,
                null
              );
              sub.addInstance(control);
            }
        }
        return groups;
    }
}
