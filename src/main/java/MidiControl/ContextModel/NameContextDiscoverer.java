package MidiControl.ContextModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.SubControl;

public class NameContextDiscoverer implements ContextDiscoverer {

    private static final Logger logger =
        Logger.getLogger(NameContextDiscoverer.class.getName());

    private static final Pattern TRAILING_NUMBER = Pattern.compile("(\\d+)$");

    @Override
    public void discover(List<Context> out, CanonicalRegistry registry) {

        logger.info("--- Discovering Name Contexts ---");

        int groupsDetected = 0;
        int contextsCreated = 0;

        for (ControlGroup group : registry.getGroups().values()) {

            List<SubControl> nameSubcontrols = getNameSubcontrols(group);

            if (nameSubcontrols.isEmpty()) {
                continue;
            }

            groupsDetected++;

            int maxChannels = nameSubcontrols.stream()
                .mapToInt(sc -> sc.getInstances().size())
                .max()
                .orElse(0);

            logger.fine(String.format("Detected name-capable group: %s  with %d subcontrols and max %s instances",
                group.getName(),nameSubcontrols.size(),maxChannels));

            String groupKey = normaliseNameGroup(group.getName());

            for (int ch = 0; ch < maxChannels; ch++) {

                List<ContextFilter> filters = new ArrayList<>();

                for (SubControl sc : nameSubcontrols) {
                    if (ch < sc.getInstances().size()) {
                        filters.add(new ContextFilter(
                            group.getName(),
                            sc.getName(),
                            ch
                        ));
                    }
                }

                if (filters.isEmpty()) {
                    continue;
                }

                Context ctx = new Context(
                    "name." + groupKey + "." + ch,
                    displayNameFor(group.getName(), ch),
                    ContextType.NAME,
                    List.of("FOH", "Monitor"),
                    filters
                );

                out.add(ctx);
                contextsCreated++;

                logger.fine("Created NAME context: " + ctx.getId()
                    + " with " + filters.size() + " filters");
            }
        }

        logger.info("Detected " + groupsDetected + " name-capable groups");
        logger.info("Created " + contextsCreated + " NAME contexts");
        logger.info("--- Finished ---");
    }

    private List<SubControl> getNameSubcontrols(ControlGroup group) {
        String groupName = group.getName();

        if (!isNameCapableGroup(groupName)) {
            return List.of();
        }

        return group.getSubcontrols().values().stream()
            .filter(sc -> isNameSubcontrol(groupName, sc.getName()))
            .sorted(Comparator.comparingInt(sc -> namePartOrder(sc.getName())))
            .toList();
    }

    private boolean isNameCapableGroup(String groupName) {
        return groupName.contains("Name")
            || groupName.contains("Input")
            || groupName.contains("Mix")
            || groupName.contains("AUX")
            || groupName.contains("Aux")
            || groupName.contains("Matrix")
            || groupName.contains("DCA");
    }

    private boolean isNameSubcontrol(String groupName, String subControlName) {
        return groupName.contains("Name");
    }

    private int namePartOrder(String subControlName) {
        Matcher matcher = TRAILING_NUMBER.matcher(subControlName);

        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        return Integer.MAX_VALUE;
    }

    private String normaliseNameGroup(String groupName) {

        if (groupName.contains("Input")) {
            return "input";
        }

        if (groupName.contains("DCA")) {
            return "dca";
        }

        if (groupName.contains("Mix")) {
            return "mix";
        }

        if (groupName.contains("Matrix")) {
            return "matrix";
        }

        if (groupName.contains("AUX")
            || groupName.contains("Aux")) {
            return "aux";
        }

        if (groupName.contains("Bus")) {
            return "bus";
        }

        return groupName
            .replaceFirst("^k", "")
            .replace("Name", "")
            .replaceAll("[^A-Za-z0-9]+", "")
            .toLowerCase();
    }

    private String displayNameFor(String groupName, int index) {
        if (groupName.contains("Input")) {
            return "Input " + (index + 1) + " Name";
        }

        if (groupName.contains("DCA")) {
            return "DCA " + (index + 1) + " Name";
        }

        if (groupName.contains("Mix") || groupName.contains("Bus")) {
            return "Mix " + (index + 1) + " Name";
        }

        if (groupName.contains("Matrix")) {
            return "Matrix " + (index + 1) + " Name";
        }

        if (groupName.contains("AUX") || groupName.contains("Aux")) {
            return "Aux " + (index + 1) + " Name";
        }

        return groupName + " Name " + (index + 1);
    }
}
