package MidiControl.ContextModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.SubControl;

public class ChannelContextDiscoverer implements ContextDiscoverer {
    private static final Logger log = Logger.getLogger(ChannelContextDiscoverer.class.getName());

    
    private static final Set<String> REAL_CHANNEL_GROUPS = Set.of(
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
        "kInputPair"
    );

    private static final Set<String> PATCH_GROUPS = Set.of(
        "kChannelInput",
        "kChannelInsertIn",
        "kChannelInsertOut",
        "kEffectInput",
        "kPatchInInput",
        "kPatchInInsertIn",
        "kPatchInInsertOut"
    );

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

            out.add(new Context(
                "channel." + ch,
                "Channel " + (ch + 1),
                ContextType.CHANNEL,
                List.of("FOH", "Monitor"),
                filters
            ));
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
