package MidiControl.ContextModel;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.SubControl;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ChannelContextDiscoverer implements ContextDiscoverer {

  private static final Set<String> REAL_CHANNEL_GROUPS =
      Set.of(
          "kInputFader",
          "kInputOn",
          "kInputPan",
          "kInputChannelPan",
          "kInputMute",
          "kInputDynamics1",
          "kInputGate",
          "kInputDynamics2",
          "kInputComp",
          "kInputHA",
          "kInputPair");

  private static final Set<String> PATCH_GROUPS =
      Set.of(
          "kChannelInput",
          "kChannelInsertIn",
          "kChannelInsertOut",
          "kEffectInput",
          "kPatchInInput",
          "kPatchInInsertIn",
          "kPatchInInsertOut");

  private static final Set<String> DETAIL_GROUPS =
      Set.of(
          "kInputChannelOn",
          "kInputEQ",
          "kInputAttenuator",
          "kInputToMix",
          "kInputAUX",
          "kInputRouting",
          "kInputGroup",
          "kSetupSoloChOn",
          "kSetupCueInput");

  @Override
  public void discover(List<Context> out, CanonicalRegistry registry) {

    int channelCount = detectRealChannelCount(registry);

    for (int ch = 0; ch < channelCount; ch++) {

      List<ContextFilter> filters = new ArrayList<>();

      for (String group : REAL_CHANNEL_GROUPS) {
        filters.add(new ContextFilter(group, "*", ch));
      }

      for (String group : PATCH_GROUPS) {
        filters.add(new ContextFilter(group, "*", ch));
      }

      for (String group : DETAIL_GROUPS) {
        filters.add(new ContextFilter(group, "*", ch));
      }

      ControlGroup mixTypes = registry.getGroup("kMixBusType");
      if (mixTypes != null && mixTypes.getSubcontrol("kBusType") != null) {
        int count = mixTypes.getSubcontrol("kBusType").getInstances().size();
        for (int mix = 0; mix < count; mix++) {
          filters.add(new ContextFilter("kMixBusType", "kBusType", mix));
        }
      }

      out.add(
          new Context(
              "channel." + ch,
              "Channel " + (ch + 1),
              ContextType.CHANNEL,
              List.of("FOH", "Monitor"),
              filters));
    }
  }

  private int detectRealChannelCount(CanonicalRegistry registry) {
    int max = 0;

    for (String groupName : REAL_CHANNEL_GROUPS) {
      ControlGroup cg = registry.getGroup(groupName);
      if (cg == null) continue;

      for (SubControl sc : cg.getSubcontrols().values()) {
        max = Math.max(max, sc.getInstances().size());
      }
    }

    return max;
  }
}
