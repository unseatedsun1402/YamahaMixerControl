package MidiControl.ContextModel.ViewBuilders;

import MidiControl.ContextModel.ChannelSource;
import MidiControl.ContextModel.Context;
import MidiControl.ContextModel.ContextFilter;
import MidiControl.ContextModel.ControlType;
import MidiControl.ContextModel.ViewBuilder;
import MidiControl.ContextModel.ViewControl;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;
import MidiControl.Controls.Widgets.ChannelOn;
import MidiControl.Controls.Widgets.Fader;
import MidiControl.Controls.Widgets.Generic;
import java.util.ArrayList;
import java.util.List;

public class SystemControlsViewBuilder implements ViewBuilder {
  String viewType = "system-controls-view";

  @Override
  public List<ViewControl> build(Context context, CanonicalRegistry registry, String suffix) {

    switch (context.getContextType()) {
      case MUTE:
        return buildMuteControls(context, registry);

      case MONITOR:
        return buildMonitorControls(context, registry);

      case DCA:
        return buildDcaControls(context, registry);

      default:
        return new ArrayList<>();
    }
  }

  private ViewControl monitorControl(ControlInstance instance, String contextId) {
    String sub = instance.getSubcontrol();
    String label;

    switch (sub) {
      case "kMonitorSelect":
        label = "Monitor source (desk code)";
        break;
      case "kMonitorMono":
        label = "Mono monitor";
        break;
      case "kMonitorCueInterruption":
        label = "Cue interruption";
        break;
      case "kMonitorOn":
        label = "Monitor on";
        break;
      case "kMonitorDim":
        label = "Monitor dim";
        break;
      case "kMonitorDimLevel":
        label = "Dim level (dB)";
        break;
      case "kMonitorLevel":
        label = "Monitor level";
        break;
      case "kCueOutOn":
        label = "Cue output";
        break;
      case "kCueLast":
        label = "Cue selection";
        break;
      case "kCueLocInput":
        label = "Input cue point";
        break;
      case "kCueLocDCA":
        label = "DCA cue point";
        break;
      case "kCueLocOutput":
        label = "Output cue point";
        break;
      case "kCueTrimInput":
        label = "Input cue trim (dB)";
        break;
      case "kCueTrimDCA":
        label = "DCA cue trim (dB)";
        break;
      case "kCueTrimOutput":
        label = "Output cue trim (dB)";
        break;
      case "kSoloSolo":
        label = "Solo enabled";
        break;
      case "kSoloSel":
        label = "Solo selection";
        break;
      case "kSoloListen":
        label = "Solo listen point";
        break;
      case "kSoloSoloLevel":
        label = "Solo trim (dB)";
        break;
      case "kSoloFollowPan":
        label = "Solo follows pan";
        break;
      default:
        label = null;
    }

    if (label == null) return null;

    String[] choices = {};
    switch (sub) {
      case "kCueLast":
        choices = new String[] {"Mix Cue", "Last Cue"};
        break;

      case "kCueLocInput":
        choices = new String[] {"PFL", "AFL", "Post Pan"};
        break;

      case "kCueLocDCA":
        choices = new String[] {"Pre Pan", "Post Pan"};
        break;

      case "kCueLocOutput":
        choices = new String[] {"PFL", "Post On"};
        break;

      case "kSoloSel":
        choices = new String[] {"Mix Solo", "Last Solo"};
        break;

      case "kSoloListen":
        choices = new String[] {"Pre Fader", "After Pan"};
        break;

      default:
        choices = null;
    }

    ControlType type =
        choices != null
                || "kMonitorSelect".equals(sub)
                || sub.startsWith("kCueTrim")
                || "kSoloSoloLevel".equals(sub)
                || "kMonitorDimLevel".equals(sub)
            ? ControlType.SELECT
            : "kMonitorLevel".equals(sub) ? ControlType.SLIDER_HORIZONTAL : ControlType.TOGGLE;
    ViewControl control =
        Generic.createGenericControl(
            instance, sub, "monitor", label, type, "system-controls-view", contextId, sub, null);
    if (choices != null) {
      control.options = options(choices);
    } else if (sub.startsWith("kCueTrim")) {
      control.options = decibels(0, 30, -20);
    } else if ("kSoloSoloLevel".equals(sub) || "kMonitorDimLevel".equals(sub)) {
      control.options = decibels(instance.getMin(), instance.getMax(), 0);
    }
    return control;
  }

  private List<ViewControl> buildMuteControls(Context context, CanonicalRegistry registry) {

    List<ViewControl> controls = new ArrayList<>();

    for (ContextFilter filter : context.getFilters()) {
      for (ControlInstance instance : registry.getInstancesForFilter(filter)) {

        String label =
            mutePrefix(instance.getGroup()) + "Mute " + (instance.getInstanceIndex() + 1);

        controls.add(createMuteControl(instance, context.getId(), label));
      }
    }

    return controls;
  }

  private String mutePrefix(String group) {
    switch (group) {
      case "kInMuteGrpMaster":
        return "Input ";

      case "kOutMuteGrpMaster":
        return "Output ";

      default:
        return "";
    }
  }

  private ViewControl createMuteControl(ControlInstance instance, String contextId, String label) {

    return Generic.createGenericControl(
        instance,
        "MUTE_MASTER",
        "mute",
        label,
        ControlType.TOGGLE,
        viewType,
        contextId,
        "MUTE_MASTER",
        null);
  }

  private List<ViewControl> buildMonitorControls(Context context, CanonicalRegistry registry) {

    List<ViewControl> controls = new ArrayList<>();

    for (ContextFilter filter : context.getFilters()) {
      for (ControlInstance instance : registry.getInstancesForFilter(filter)) {

        ViewControl control = monitorControl(instance, context.getId());

        if (control != null) {
          controls.add(control);
        }
      }
    }

    return controls;
  }

  private List<ViewControl> buildDcaControls(Context context, CanonicalRegistry registry) {

    List<ViewControl> controls = new ArrayList<>();

    for (ContextFilter filter : context.getFilters()) {
      for (ControlInstance instance : registry.getInstancesForFilter(filter)) {

        ViewControl fader = dcaControl(instance, context.getId(), instance.getInstanceIndex());

        if (fader != null) {
          controls.add(fader);
        }
      }
    }

    return controls;
  }

  private ViewControl dcaControl(ControlInstance instance, String contextId, int dcaIndex) {

    ViewControl control;

    control = buildDcaFader(instance, contextId);
    if (control != null) return control;

    control = buildDcaMute(instance, contextId, dcaIndex);
    if (control != null) return control;

    return null;
  }

  private ViewControl buildDcaFader(ControlInstance instance, String contextId) {

    if (!"kDCAFader".equals(instance.getGroup())) {
      return null;
    }

    if (!"kFader".equals(instance.getSubcontrol())) {
      return null;
    }

    return Fader.createDcaFader(instance, viewType, contextId);
  }

  private ViewControl buildDcaMute(ControlInstance instance, String contextId, int dcaIndex) {

    if (!"kDCAMute".equals(instance.getGroup())) {
      return null;
    }

    if (!"kDCAMuteOn".equals(instance.getSubcontrol())) {
      return null;
    }

    return ChannelOn.createDcaOn(instance, viewType, contextId, dcaIndex);
  }

  private List<ChannelSource> options(String[] labels) {
    List<ChannelSource> options = new ArrayList<>();
    for (int i = 0; i < labels.length; i++) {
      ChannelSource option = new ChannelSource();
      option.source = i;
      option.name = labels[i];
      options.add(option);
    }
    return options;
  }

  private List<ChannelSource> decibels(int min, int max, int offset) {
    List<ChannelSource> options = new ArrayList<>();
    for (int i = min; i <= max; i++) {
      ChannelSource option = new ChannelSource();
      option.source = i;
      option.name = (i + offset) + " dB";
      options.add(option);
    }
    return options;
  }
}
