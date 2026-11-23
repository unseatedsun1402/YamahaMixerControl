package MidiControl.ContextModel;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.SubControl;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class DcaContextDiscoverer implements ContextDiscoverer {
  private static final Logger logger = Logger.getLogger(DcaContextDiscoverer.class.getName());

  @Override
  public void discover(List<Context> out, CanonicalRegistry registry) {

    ControlGroup group = registry.getGroup("kDCAFader");

    if (group == null) {
      return;
    }

    SubControl sub = group.getSubcontrol("kFader");

    if (sub == null) {
      logger.warning("Sub should not be null if a dca top group can be found");
      return;
    }

    int dcaCount = sub.getInstances().size();

    for (int i = 0; i < dcaCount; i++) {

      List<ContextFilter> filters = new ArrayList<>();

      filters.add(new ContextFilter("kDCAFader", "kFader", i));
      filters.add(new ContextFilter("kDCAMute", "kDCAMuteOn", i));
      filters.add(new ContextFilter("kDCAName", "kNameShort1", i));
      filters.add(new ContextFilter("kDCAName", "kNameShort2", i));

      out.add(new Context("dca." + i, "DCA " + (i + 1), ContextType.DCA, List.of(), filters));
    }

    logger.info(String.format("Discovered %d DCA objects", out.size()));
  }
}
