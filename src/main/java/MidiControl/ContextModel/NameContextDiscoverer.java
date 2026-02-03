package MidiControl.ContextModel;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.SubControl;

public class NameContextDiscoverer implements ContextDiscoverer {

    private static final Logger logger = Logger.getLogger(NameContextDiscoverer.class.getName());

    @Override
    public void discover(List<Context> out, CanonicalRegistry registry) {

        logger.info("--- Discovering Name Contexts ---");

        List<ControlGroup> groups = new ArrayList<>();
        int maxChannels = 0;

        // --------------------------------------------
        // 1. Find groups that have Name subcontrols
        // --------------------------------------------
        for (ControlGroup group : registry.getGroups().values()) {

            if (!isNameCapableGroup(group.getName()))
                continue;

            boolean ok = group.getSubcontrols().values().stream()
                .anyMatch(sc -> sc.getName().contains("Name"));

            if (!ok)
                continue;

            groups.add(group);

            // Find maximum instance count among its name subcontrols
            int max = group.getSubcontrols().values().stream()
                .filter(sc -> sc.getName().contains("Name"))
                .mapToInt(sc -> sc.getInstances().size())
                .max().orElse(0);

            maxChannels = Math.max(maxChannels, max);

            logger.info("Detected name-capable group: " + group.getName()
                + " with max " + max + " instances");
        }

        logger.info("Detected " + groups.size() + " name contexts ");

        if (groups.isEmpty()) {
            logger.info("No name-capable groups detected.");
            return;
        }

        logger.info("Max name channels = " + maxChannels);

        // --------------------------------------------
        // 2. Create one NAME context per channel index
        // --------------------------------------------
        for (int ch = 0; ch < maxChannels; ch++) {

            List<ContextFilter> filters = new ArrayList<>();

            for (ControlGroup group : groups) {
                for (SubControl sc : group.getSubcontrols().values()) {

                    if (!sc.getName().contains("Name"))
                        continue;

                    if (ch < sc.getInstances().size()) {
                        filters.add(new ContextFilter(
                            group.getName(),
                            sc.getName(),   // the single SubControl for all characters
                            ch              // instance index = char slot
                        ));
                    }
                }
            }

            if (!filters.isEmpty()) {
                Context ctx = new Context(
                    "name." + ch,
                    "Name " + (ch + 1),
                    ContextType.NAME,
                    List.of("FOH", "Monitor"),
                    filters
                );

                out.add(ctx);
                logger.fine("Created NAME context: " + ctx.getId());
            }
        }

        logger.info("--- Finished ---");
    }

    private boolean isNameCapableGroup(String groupName) {
        return groupName.contains("Input");
            // || groupName.contains("Mix")
            // || groupName.contains("AUX")
            // || groupName.contains("Matrix")
            // || groupName.contains("DCA")
            // || groupName.contains("Name");
    }
}