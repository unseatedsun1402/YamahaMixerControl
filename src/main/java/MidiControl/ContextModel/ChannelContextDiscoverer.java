package MidiControl.ContextModel;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.SubControl;

public class ChannelContextDiscoverer implements ContextDiscoverer {
    private static final Logger log = Logger.getLogger(ChannelContextDiscoverer.class.getName());
    
    @Override
    public void discover(List<Context> out, CanonicalRegistry registry) {
        log.info("--- Discovering Channels ---");

        List<ControlGroup> perChannelGroups = new ArrayList<>();
        int channelCount = 0;

        for (ControlGroup group : registry.getGroups().values()) {

            // Only input-related groups can define channels
            if (!group.getName().startsWith("kInput"))
                continue;

            boolean isPerChannel = false;

            for (SubControl sub : group.getSubcontrols().values()) {

                int count = sub.getInstances().size();

                // Ignore huge tables (patch, library, scene, etc.)
                if (count > 128)
                    continue;

                if (count > 1) {
                    isPerChannel = true;
                    channelCount = Math.max(channelCount, count);
                }
            }

            if (isPerChannel) {
                perChannelGroups.add(group);
                log.fine("Per-channel group detected: " + group.getName());
            }
        }

        log.info("Detected channel count: " + channelCount);

        for (int ch = 0; ch < channelCount; ch++) {

            List<ContextFilter> filters = new ArrayList<>();

            for (ControlGroup group : perChannelGroups) {
                filters.add(new ContextFilter(group.getName(), "*", ch));
            }

            out.add(new Context(
                "channel." + ch,
                "Channel " + (ch + 1),
                ContextType.CHANNEL,
                List.of("FOH", "Monitor"),
                filters
            ));

            log.fine("Created context: channel." + ch);
        }
    }
}
