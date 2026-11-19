package MidiControl.ContextModel.ViewBuilders;

import MidiControl.ContextModel.ChannelSource;
import MidiControl.ContextModel.Context;
import MidiControl.ContextModel.ContextFilter;
import MidiControl.ContextModel.ContextType;
import MidiControl.ContextModel.ControlType;
import MidiControl.ContextModel.ViewBuilder;
import MidiControl.ContextModel.ViewControl;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;
import MidiControl.Controls.Widgets.Generic;
import java.util.ArrayList;
import java.util.List;

public class SystemControlsViewBuilder implements ViewBuilder {
  @Override
  public List<ViewControl> build(Context context, CanonicalRegistry registry, String suffix) {
    List<ViewControl> controls = new ArrayList<>();
    for (ContextFilter filter : context.getFilters()) {
      for (ControlInstance instance : registry.getInstancesForFilter(filter)) {
        if (context.getContextType() == ContextType.MUTE) {
          String prefix = switch (instance.getGroup()) {
            case "kInMuteGrpMaster" -> "Input ";
            case "kOutMuteGrpMaster" -> "Output ";
            default -> "";
          };
          String label = prefix + "Mute " + (instance.getInstanceIndex() + 1);
          controls.add(Generic.createGenericControl(instance, "MUTE_MASTER", "mute",
              label, ControlType.TOGGLE, "system-controls-view", context.getId(),
              "MUTE_MASTER", null));
        } else if (context.getContextType() == ContextType.MONITOR) {
          ViewControl control = monitorControl(instance, context.getId());
          if (control != null) controls.add(control);
        }
      }
    }
    return controls;
  }

  private ViewControl monitorControl(ControlInstance instance, String contextId) {
    String sub = instance.getSubcontrol();
    String label = switch (sub) {
      case "kMonitorSelect" -> "Monitor source (desk code)";
      case "kMonitorMono" -> "Mono monitor";
      case "kMonitorCueInterruption" -> "Cue interruption";
      case "kMonitorOn" -> "Monitor on";
      case "kMonitorDim" -> "Monitor dim";
      case "kMonitorDimLevel" -> "Dim level (dB)";
      case "kMonitorLevel" -> "Monitor level";
      case "kCueOutOn" -> "Cue output";
      case "kCueLast" -> "Cue selection";
      case "kCueLocInput" -> "Input cue point";
      case "kCueLocDCA" -> "DCA cue point";
      case "kCueLocOutput" -> "Output cue point";
      case "kCueTrimInput" -> "Input cue trim (dB)";
      case "kCueTrimDCA" -> "DCA cue trim (dB)";
      case "kCueTrimOutput" -> "Output cue trim (dB)";
      case "kSoloSolo" -> "Solo enabled";
      case "kSoloSel" -> "Solo selection";
      case "kSoloListen" -> "Solo listen point";
      case "kSoloSoloLevel" -> "Solo trim (dB)";
      case "kSoloFollowPan" -> "Solo follows pan";
      default -> null;
    };
    if (label == null) return null;
    String[] choices = switch (sub) {
      case "kCueLast" -> new String[] {"Mix Cue", "Last Cue"};
      case "kCueLocInput" -> new String[] {"PFL", "AFL", "Post Pan"};
      case "kCueLocDCA" -> new String[] {"Pre Pan", "Post Pan"};
      case "kCueLocOutput" -> new String[] {"PFL", "Post On"};
      case "kSoloSel" -> new String[] {"Mix Solo", "Last Solo"};
      case "kSoloListen" -> new String[] {"Pre Fader", "After Pan"};
      default -> null;
    };
    ControlType type = choices != null || "kMonitorSelect".equals(sub)
        || sub.startsWith("kCueTrim") || "kSoloSoloLevel".equals(sub)
        || "kMonitorDimLevel".equals(sub)
        ? ControlType.SELECT
        : "kMonitorLevel".equals(sub) ? ControlType.SLIDER_HORIZONTAL : ControlType.TOGGLE;
    ViewControl control = Generic.createGenericControl(instance, sub, "monitor",
        label, type, "system-controls-view", contextId, sub, null);
    if (choices != null) {
      control.options = options(choices);
    } else if (sub.startsWith("kCueTrim")) {
      control.options = decibels(0, 30, -20);
    } else if ("kSoloSoloLevel".equals(sub) || "kMonitorDimLevel".equals(sub)) {
      control.options = decibels(instance.getMin(), instance.getMax(), 0);
    }
    return control;
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
