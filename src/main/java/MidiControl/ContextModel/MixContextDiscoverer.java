package MidiControl.ContextModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.SubControl;

public class MixContextDiscoverer implements ContextDiscoverer{
    private static final Pattern MIX_PATTERN =
        Pattern.compile("^[A-Za-z]+(?<mix>\\d+)(?<param>[A-Za-z]+)$");
    private static final Logger log = Logger.getLogger(MixContextDiscoverer.class.getName());

    @Override
    public void discover(List<Context> out, CanonicalRegistry registry) {
        log.info("--- Discovering Mixes ---");

        // Key = contextId suffix ("3", "3-4", "9-10"), value = mappings
        Map<String, List<MixMapping>> mixMap = new HashMap<>();
        Map<String, String> mixLabels = new HashMap<>();

        for (ControlGroup group : registry.getGroups().values()) {

            String groupName = group.getName();

            // Only groups that can legitimately contain mix sends
            if (!(groupName.contains("ToMix") || groupName.contains("AUX") || groupName.startsWith("kMix")))
                continue;

            for (SubControl sub : group.getSubcontrols().values()) {

                Matcher m = MIX_PATTERN.matcher(sub.getName());
                if (!m.matches())
                    continue;

                String mixDigits = m.group("mix");
                String param     = m.group("param");

                BusInfo bus = parseBusDigits(mixDigits, "Mix");
                if (bus == null) {
                    log.fine("Ignoring unparseable mix index '" + mixDigits +
                             "' from subcontrol " + sub.getName());
                    continue;
                }

                String contextSuffix = bus.contextSuffix();   // "3" or "3-4"
                String label         = bus.displayLabel();    // "Mix 3" or "Mix 3/4"

                String prefix = sub.getName().substring(
                    0, sub.getName().length() - param.length()
                );

                mixMap.computeIfAbsent(contextSuffix, k -> new ArrayList<>())
                      .add(new MixMapping(groupName, prefix));

                mixLabels.putIfAbsent(contextSuffix, label);

                log.fine("Mix send detected: group=" + groupName
                    + " sub=" + sub.getName()
                    + " → " + label + " (suffix=" + contextSuffix + ")");
            }
        }

        for (var entry : mixMap.entrySet()) {
            String suffix = entry.getKey();
            List<MixMapping> mappings = entry.getValue();
            String label = mixLabels.getOrDefault(suffix, "Mix " + suffix);

            List<ContextFilter> filters = new ArrayList<>();
            for (MixMapping m : mappings) {
                filters.add(new ContextFilter(m.groupName, m.prefix + "*", null));
            }

            String contextId = "mix." + suffix;

            out.add(new Context(
                contextId,
                label,
                ContextType.MIX,
                List.of("Musician", "Monitor", "FOH"),
                filters
            ));

            log.info("Created context: " + contextId + " (" + label + ")");
        }
    }

    private static class MixMapping {
        final String groupName;
        final String prefix;

        MixMapping(String groupName, String prefix) {
            this.groupName = groupName;
            this.prefix = prefix;
        }
    }

    private static class BusInfo {
        final int left;
        final Integer right; // null for mono
        final String kindLabel; // "Mix" or "Matrix"

        BusInfo(int left, Integer right, String kindLabel) {
            this.left = left;
            this.right = right;
            this.kindLabel = kindLabel;
        }

        boolean isStereo() {
            return right != null;
        }

        String contextSuffix() {
            return isStereo() ? left + "-" + right : String.valueOf(left);
        }

        String displayLabel() {
            return isStereo()
                ? kindLabel + " " + left + "/" + right
                : kindLabel + " " + left;
        }
    }

    private BusInfo parseBusDigits(String digits, String kindLabel) {

        // Pure mono: "1", "2", "15", etc.
        if (digits.length() <= 2) {
            int index = Integer.parseInt(digits);
            return new BusInfo(index, null, kindLabel);
        }

        // Stereo pairs on M7CL etc: "0304", "0910", "1314", "1516"
        if (digits.length() == 4) {
            String leftStr  = digits.substring(0, 2);
            String rightStr = digits.substring(2);

            int left  = Integer.parseInt(leftStr);
            int right = Integer.parseInt(rightStr);

            return new BusInfo(left, right, kindLabel);
        }

        // Anything else is suspicious (e.g., "102" with 3 digits)
        log.fine("Unrecognized bus digit pattern '" + digits + "' for " + kindLabel);
        return null;
    }
}
