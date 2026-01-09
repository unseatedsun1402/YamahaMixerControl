package MidiControl.ContextModel;

import java.util.HashSet;
import java.util.Set;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.SubControl;

public class ControlSchema {

    private final CanonicalRegistry registry;

    public ControlSchema(CanonicalRegistry registry) {
        this.registry = registry;
    }

    public Set<String> getPrefixesForGroup(String groupName) {
        Set<String> prefixes = new HashSet<>();

        ControlGroup group = registry.getGroup(groupName);
        if (group == null)
            return prefixes;

        for (SubControl sub : group.getSubcontrols().values()) {
            String name = sub.getName();

            // Extract prefix up to first digit or uppercase transition
            String prefix = extractPrefix(name);
            if (prefix != null)
                prefixes.add(prefix);
        }

        return prefixes;
    }

    public Set<String> getGroupsForPrefix(String prefix) {
        Set<String> groups = new HashSet<>();

        for (ControlGroup group : registry.getGroups().values()) {
            for (SubControl sub : group.getSubcontrols().values()) {
                if (sub.getName().startsWith(prefix)) {
                    groups.add(group.getName());
                    break;
                }
            }
        }

        return groups;
    }

    public Set<String> getAllPrefixes() {
        Set<String> prefixes = new HashSet<>();

        for (ControlGroup group : registry.getGroups().values()) {
            for (SubControl sub : group.getSubcontrols().values()) {
                String prefix = extractPrefix(sub.getName());
                if (prefix != null)
                    prefixes.add(prefix);
            }
        }

        return prefixes;
    }

    public int detectIndexedRange(String groupName, String prefix) {
        ControlGroup group = registry.getGroup(groupName);
        if (group == null)
            return 0;

        int maxIndex = 0;

        for (SubControl sub : group.getSubcontrols().values()) {
            String name = sub.getName();
            if (!name.startsWith(prefix))
                continue;

            Integer idx = extractIndex(name);
            if (idx != null && idx > maxIndex)
                maxIndex = idx;
        }

        return maxIndex;
    }


    private String extractPrefix(String name) {
        // Prefix ends before the first digit
        for (int i = 0; i < name.length(); i++) {
            if (Character.isDigit(name.charAt(i))) {
                return name.substring(0, i);
            }
        }
        return name; // no digits → whole name is prefix
    }

    private Integer extractIndex(String name) {
        int start = -1, end = -1;

        for (int i = 0; i < name.length(); i++) {
            if (Character.isDigit(name.charAt(i))) {
                if (start == -1) start = i;
                end = i + 1;
            } else if (start != -1) {
                break;
            }
        }

        if (start == -1 || end == -1)
            return null;

        try {
            return Integer.parseInt(name.substring(start, end));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public int detectIndexedRangeForAnyGroup(String prefix) {
        int max = 0;

        for (ControlGroup group : registry.getGroups().values()) {
            for (SubControl sub : group.getSubcontrols().values()) {

                String name = sub.getName();
                if (!name.startsWith(prefix))
                    continue;

                // Extract index from name (kFader1 → 1)
                Integer idx = extractIndex(name);
                if (idx != null && idx > max)
                    max = idx;
            }
        }

        return max;
    }
}