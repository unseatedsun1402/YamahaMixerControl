package MidiControl.unit;

import MidiControl.SysexUtils.SysexMapping;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.SubControl;
import MidiControl.Controls.ControlInstance;

import java.util.List;

public class BroadcastBufferTest {

    private ControlInstance findWritableInstance(CanonicalRegistry registry) {
        for (ControlGroup group : registry.getGroups().values()) {
            for (SubControl sub : group.getSubcontrols().values()) {
                for (ControlInstance ci : sub.getInstances()) {

                    if (!ci.hasChangeMapping()) continue;

                    SysexMapping m = ci.getSysex();
                    if (m == null) continue;

                    // Must have at least one value byte index
                    if (m.getValueByteIndices() == null || m.getValueByteIndices().length == 0)
                        continue;

                    // Must contain dd token
                    if (!m.getParameter_change_format().contains("dd"))
                        continue;

                    return ci;
                }
            }
        }
        return null;
    }

    private ControlInstance findRequestableInstance(CanonicalRegistry registry) {
        for (ControlGroup group : registry.getGroups().values()) {
            for (SubControl sub : group.getSubcontrols().values()) {
                for (ControlInstance ci : sub.getInstances()) {

                    if (!ci.hasRequestMapping()) continue;

                    SysexMapping m = ci.getSysex();
                    if (m == null) continue;

                    List<String> fmt = m.getParameter_request_format();
                    if (fmt == null || fmt.isEmpty()) continue;

                    // Must contain at least one literal or cc/dd token
                    if (fmt.stream().anyMatch(tok -> tok instanceof String)) {
                        return ci;
                    }
                }
            }
        }
        return null;
    }
}