package MidiControl.ContextModel;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.SubControl;
import java.util.ArrayList;
import java.util.List;

public class MuteContextDiscoverer implements ContextDiscoverer {
  @Override
  public void discover(List<Context> out, CanonicalRegistry registry) {
    List<ContextFilter> filters = new ArrayList<>();
    add(filters, registry, "kSceneMuteMaster", "kMuteMaster");
    add(filters, registry, "kInMuteGrpMaster", "kMasterMute");
    add(filters, registry, "kOutMuteGrpMaster", "kMasterMute");
    if (!filters.isEmpty()) {
      out.add(
          new Context(
              "mute.0", "Mute groups", ContextType.MUTE, List.of("FOH", "Monitor"), filters));
    }
  }

  private void add(
      List<ContextFilter> filters, CanonicalRegistry registry, String groupName, String subName) {
    ControlGroup group = registry.getGroup(groupName);
    SubControl sub = group == null ? null : group.getSubcontrol(subName);
    if (sub == null) return;
    for (int i = 0; i < sub.getInstances().size(); i++) {
      filters.add(new ContextFilter(groupName, subName, i));
    }
  }
}
