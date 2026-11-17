package MidiControl.ContextModel;

import MidiControl.Controls.CanonicalRegistry;
import java.util.ArrayList;
import java.util.List;

public class MonitorContextDiscoverer implements ContextDiscoverer {
  private static final String[][] CONTROLS = {
    {"kSetupMonitor", "kMonitorSelect"},
    {"kSetupMonitor", "kMonitorMono"},
    {"kSetupMonitor", "kMonitorCueInterruption"},
    {"kSetupMonitor", "kMonitorOn"},
    {"kSetupMonitor", "kMonitorDim"},
    {"kSetupMonitor", "kMonitorDimLevel"},
    {"kSetupMonitor", "kMonitorLevel"},
    {"kSetupCue", "kCueOutOn"},
    {"kSetupCue", "kCueLast"},
    {"kSetupCue", "kCueLocInput"},
    {"kSetupCue", "kCueLocDCA"},
    {"kSetupCue", "kCueLocOutput"},
    {"kSetupCue", "kCueTrimInput"},
    {"kSetupCue", "kCueTrimDCA"},
    {"kSetupCue", "kCueTrimOutput"},
    {"kSetupSolo", "kSoloSolo"},
    {"kSetupSolo", "kSoloSel"},
    {"kSetupSolo", "kSoloListen"},
    {"kSetupSolo", "kSoloSoloLevel"},
    {"kSetupSolo", "kSoloFollowPan"}
  };

  @Override
  public void discover(List<Context> out, CanonicalRegistry registry) {
    List<ContextFilter> filters = new ArrayList<>();
    for (String[] control : CONTROLS) {
      if (registry.find(0, control[0], control[1]) != null) {
        filters.add(new ContextFilter(control[0], control[1], 0));
      }
    }
    if (!filters.isEmpty()) {
      out.add(
          new Context(
              "monitor.0", "Monitor", ContextType.MONITOR, List.of("FOH", "Monitor"), filters));
    }
  }
}
