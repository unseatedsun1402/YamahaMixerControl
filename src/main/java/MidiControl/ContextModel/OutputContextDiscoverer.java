package MidiControl.ContextModel;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.SubControl;

import java.util.*;
import java.util.logging.Logger;

public class OutputContextDiscoverer implements ContextDiscoverer {

    private static final Logger log = Logger.getLogger(OutputContextDiscoverer.class.getName());

    @Override
    public void discover(List<Context> out, CanonicalRegistry registry) {
        log.info("--- Discovering Outputs (Corrected Yamaha Heuristics) ---");

        discoverStereoLR(out, registry);
        discoverOmniOutputs(out, registry);   // optional but useful
    }

    private void discoverStereoLR(List<Context> out, CanonicalRegistry registry) {

        Optional<OutputMapping> stereo = findStereoOutMapping(registry);

        if (stereo.isEmpty()) {
            log.fine("No stereo LR mapping found.");
            return;
        }

        OutputMapping m = stereo.get();

        Context lr = new Context(
            "bus.LR",
            "Stereo LR",
            ContextType.BUS,
            List.of("FOH", "Monitor"),
            List.of(new ContextFilter(m.groupName, m.prefix + "*", null))
        );

        out.add(lr);
        log.info("Created context: bus.LR (Stereo LR)");
    }

    private Optional<OutputMapping> findStereoOutMapping(CanonicalRegistry registry) {

        for (ControlGroup group : registry.getGroups().values()) {

            String g = group.getName().toLowerCase();

            // Yamaha patterns for LR:
            // kST, kStereoOut, kMain, kSTLevel, kSTOn
            boolean looksStereo =
                    g.contains("st") ||
                    g.contains("stereo") ||
                    g.contains("main");

            if (!looksStereo)
                continue;

            for (SubControl sub : group.getSubcontrols().values()) {

                String s = sub.getName().toLowerCase();

                if (s.contains("st") || s.contains("stereo") || s.contains("main")) {

                    int split = findParamStartIndex(sub.getName());
                    if (split <= 0 || split >= sub.getName().length())
                        continue;

                    String prefix = sub.getName().substring(0, split);
                    return Optional.of(new OutputMapping(group.getName(), prefix));
                }
            }
        }

        return Optional.empty();
    }

    private int findParamStartIndex(String name) {
        int i = name.length() - 1;
        while (i >= 0 && Character.isLetter(name.charAt(i))) {
            i--;
        }
        return i + 1;
    }

    private void discoverOmniOutputs(List<Context> out, CanonicalRegistry registry) {

        Map<Integer, List<OutputMapping>> omniMap = new HashMap<>();

        for (ControlGroup group : registry.getGroups().values()) {

            String g = group.getName().toLowerCase();

            // Yamaha patterns:
            // kOmniOut1, kOmniOut2, kOmniOut16
            if (!g.contains("omni"))
                continue;

            for (SubControl sub : group.getSubcontrols().values()) {

                OmniIndexInfo info = tryParseOmniIndex(sub.getName());
                if (info == null)
                    continue;

                int omniIndex = info.index;
                if (omniIndex < 1 || omniIndex > 16)
                    continue;

                String prefix = sub.getName().substring(
                        0, sub.getName().length() - info.param.length()
                );

                omniMap.computeIfAbsent(omniIndex, k -> new ArrayList<>())
                       .add(new OutputMapping(group.getName(), prefix));
            }
        }

        for (var entry : omniMap.entrySet()) {
            int index = entry.getKey();
            List<OutputMapping> mappings = entry.getValue();

            List<ContextFilter> filters = new ArrayList<>();
            for (OutputMapping m : mappings) {
                filters.add(new ContextFilter(m.groupName, m.prefix + "*", null));
            }

            Context omni = new Context(
                "omni." + index,
                "Omni Out " + index,
                ContextType.BUS,
                List.of("FOH", "Monitor"),
                filters
            );

            out.add(omni);
            log.info("Created context: omni." + index + " (Omni Out " + index + ")");
        }
    }

    private OmniIndexInfo tryParseOmniIndex(String subName) {

        // Look for patterns like:
        // kOmniOut1Level
        // kOmniOut12On
        // kOmniOut16Pan

        if (!subName.toLowerCase().contains("omni"))
            return null;

        int start = -1, end = -1;

        for (int i = 0; i < subName.length(); i++) {
            if (Character.isDigit(subName.charAt(i))) {
                if (start == -1) start = i;
                end = i + 1;
            } else if (start != -1) break;
        }

        if (start == -1 || end == -1)
            return null;

        String digits = subName.substring(start, end);
        String param = subName.substring(end);

        try {
            return new OmniIndexInfo(Integer.parseInt(digits), param);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static class OutputMapping {
        final String groupName;
        final String prefix;

        OutputMapping(String groupName, String prefix) {
            this.groupName = groupName;
            this.prefix = prefix;
        }
    }

    private static class OmniIndexInfo {
        final int index;
        final String param;

        OmniIndexInfo(int index, String param) {
            this.index = index;
            this.param = param;
        }
    }
}