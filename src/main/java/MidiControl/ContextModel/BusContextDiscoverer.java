package MidiControl.ContextModel;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.SubControl;

import java.util.*;
import java.util.logging.Logger;

/**
 * Discovers all bus-type contexts (Mix, Aux, Bus, Matrix, Stereo, Mono).
 *
 * Bus-owned groups:
 *   - kMix*     (M7CL/LS9/CL/QL mixes)
 *   - kAUX*     (01V96i aux mixes)
 *   - kBus*     (01V96i bus outputs)
 *   - kMatrix*  (matrix outputs)
 *   - kStereo*  (stereo master)
 *   - kMono*    (mono master)
 *
 * Input-owned sends (kInputToMix, kInputAUX, kInputToBus, kInputToMatrix)
 * are intentionally ignored here and handled by input view builders.
 */
public class BusContextDiscoverer implements ContextDiscoverer {

    private static final Logger log = Logger.getLogger(BusContextDiscoverer.class.getName());

    private enum BusFamily {
        MIX("kMix", "mix", "Mix"),
        AUX("kAUX", "aux", "Aux"),
        BUS("kBus", "bus", "Bus"),
        MATRIX("kMatrix", "matrix", "Matrix"),
        STEREO("kStereo", "stereo", "Stereo");

        final String groupPrefix;
        final String idPrefix;
        final String labelBase;

        BusFamily(String groupPrefix, String idPrefix, String labelBase) {
            this.groupPrefix = groupPrefix;
            this.idPrefix = idPrefix;
            this.labelBase = labelBase;
        }

        static Optional<BusFamily> fromGroupName(String groupName) {
            for (BusFamily family : values()) {
                if (groupName.startsWith(family.groupPrefix)) {
                    return Optional.of(family);
                }
            }
            return Optional.empty();
        }
    }

    @Override
    public void discover(List<Context> out, CanonicalRegistry registry) {
        log.info("--- Discovering bus contexts (Mix/Aux/Bus/Matrix/Stereo/Mono) ---");

        // 1. Group control groups by bus family
        Map<BusFamily, List<ControlGroup>> familyGroups = new EnumMap<>(BusFamily.class);
        for (ControlGroup group : registry.getGroups().values()) {
            String groupName = group.getName();
            Optional<BusFamily> familyOpt = BusFamily.fromGroupName(groupName);
            if (familyOpt.isEmpty()) {
                continue; // Not a bus-owned group
            }

            BusFamily family = familyOpt.get();
            familyGroups.computeIfAbsent(family, k -> new ArrayList<>()).add(group);
        }

        // 2. For each family, compute bus count and create contexts
        for (Map.Entry<BusFamily, List<ControlGroup>> entry : familyGroups.entrySet()) {
            BusFamily family = entry.getKey();
            List<ControlGroup> groups = entry.getValue();

            int busCount = computeBusCount(groups);
            if (busCount <= 0) {
                log.fine("No instances found for bus family " + family + "; skipping.");
                continue;
            }

            log.info("Detected " + busCount + " buses for family " + family.name());

            for (int index = 0; index < busCount; index++) {
                List<ContextFilter> filters = new ArrayList<>();

                // One filter per group in the family, bound to this bus index
                for (ControlGroup group : groups) {
                    filters.add(new ContextFilter(group.getName(), "*", index));
                }

                String contextId = family.idPrefix + "." + index;
                String label = buildLabel(family, index, busCount);

                out.add(new Context(
                    contextId,
                    label,
                    ContextType.MIX, // You can refine this per family later if desired
                    List.of("Musician", "Monitor", "FOH"),
                    filters
                ));

                log.fine("Created bus context: " + contextId + " (" + label + ")");
            }
        }
    }

    /**
     * Computes the maximum instance count across all groups' subcontrols for a bus family.
     * This matches the "one instance per bus" model used by Yamaha.
     */
    private int computeBusCount(List<ControlGroup> groups) {
        int max = 0;
        for (ControlGroup group : groups) {
            for (SubControl sub : group.getSubcontrols().values()) {
                int count = sub.getInstances().size();
                if (count > max) {
                    max = count;
                }
            }
        }
        return max;
    }

    /**
     * Builds a label for the given bus. For now, everything is treated as mono:
     *   Mix 1, Aux 1, Bus 1, Matrix 1, Stereo 1
     */
    private String buildLabel(BusFamily family, int index, int busCount) {
        // Simple, consistent, 1-based labeling
        return family.labelBase + " " + (index + 1);
    }
}