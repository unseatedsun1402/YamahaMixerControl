package MidiControl.ContextModel;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.SubControl;

public class OutputContextDiscoverer implements ContextDiscoverer {

    private static final Logger logger = Logger.getLogger(OutputContextDiscoverer.class.getName());

    private enum OutputFamily {
        OMNI("kOmniOutput", "kOmniOut", "omniout", "Omni Out"),
        SLOT("kSlotOutput", "kSlotOut", "slotout", "Slot Out"),
        DIGITAL2TR("kDigital2trOut", "kDigital2trOutIndex", "digital2tr", "2TR Digital"),
        DIRECT("kDirectOut", "kDirectOutIndex", "directout", "Direct Out"),
        GEQ("kGEQInsertion", "kGEQInsert", "geqinsert", "GEQ Insert"),
        BUSINSERT("kBusInsertInput", "kBusInsertIn", "businsert", "Bus Insert Tap"),
        AUXINSERT("kAUXInsertInput", "kAUXInsertIn", "auxinsert", "Aux Insert Tap"),
        STEREOINSERT("kStereoInsertInput", "kStereoInsertIn", "stereoinsert", "Stereo Insert Tap");

        final String groupName;
        final String subPrefix;
        final String contextPrefix;
        final String labelBase;

        OutputFamily(String groupName, String subPrefix, String contextPrefix, String labelBase) {
            this.groupName = groupName;
            this.subPrefix = subPrefix;
            this.contextPrefix = contextPrefix;
            this.labelBase = labelBase;
        }
    }

    @Override
    public void discover(List<Context> out, CanonicalRegistry registry) {

        logger.info("--- Discovering Output Contexts (family-based) ---");

        for (OutputFamily family : OutputFamily.values()) {
            discoverFamily(out, registry, family);
        }
    }

    private void discoverFamily(List<Context> out,
                                CanonicalRegistry registry,
                                OutputFamily family) {

        ControlGroup group = registry.getGroup(family.groupName);
        if (group == null) {
            logger.info("No group found for " + family.groupName);
            return;
        }

        // Get ALL matching subcontrols
        List<SubControl> subs = partialMatch(group, family.subPrefix);

        if (subs.isEmpty()) {
            for (SubControl sub : group.getSubcontrols().values())
                logger.info(String.format("has sc %s", sub.getName()));

            logger.info("No subcontrols starting with " + family.subPrefix + " in " + family.groupName);
            return;
        }


        Map<Integer, List<SubControl>> grouped = groupByMiddleNumber(subs, family.subPrefix);
        int globalIdx = 0;
        for (var entry : grouped.entrySet()) {
            
            List<SubControl> slotSubs = entry.getValue();

            slotSubs.sort(Comparator.comparing(SubControl::getName));

            for (SubControl sc : slotSubs) {
                addContextsForSubControl(sc, out, family, globalIdx);
            }
        }

    }

    private void addContextsForSubControl(SubControl sc, List<Context> out, OutputFamily family, int startIndex) {
        
        String subname =sc.getName();
        
        int count = sc.getInstances().size();
        if (count == 0) {
            logger.info("No instances for " + family.groupName + "." + subname);
            return;
        }
        
        logger.info("Discovered " + count + " outputs for family " + subname);

        for (int idx = 0; idx < count; idx++) {

            int contextIndex = startIndex + idx;

            String contextId = subname + "." + contextIndex;
            String label = buildLabel(family, contextIndex);

            List<ContextFilter> filters = List.of(
                new ContextFilter(family.groupName, subname, idx)
            );

            Context ctx = new Context(
                contextId,
                label,
                ContextType.OUTPUT,
                List.of("FOH", "Monitor"),
                filters
            );

            out.add(ctx);
            logger.fine("Added " + contextId + " for " + family.groupName);
        }
    }

    private List<SubControl> partialMatch(ControlGroup group, String match) {
        return group.getSubcontrols().values().stream()
            .filter(sub -> sub.getName().startsWith(match))
            .sorted((a, b) -> a.getName().compareTo(b.getName()))
            .collect(Collectors.toList());
    }

    private Integer extractMiddleNumber(String name) {
        var m = name.replaceAll("^k[A-Za-z]+(\\d+).*$", "$1");
        return m.equals(name) ? null : Integer.parseInt(m);
    }


    private Map<Integer, List<SubControl>> groupByMiddleNumber(List<SubControl> subs, String prefix) {
        return subs.stream()
            .filter(sc -> extractMiddleNumber(sc.getName()) != null)
            .collect(Collectors.groupingBy(sc -> extractMiddleNumber(sc.getName())));
    }



    private String buildLabel(OutputFamily family, int idx) {
        return switch (family) {
            case OMNI -> family.labelBase + " " + (idx + 1);
            case SLOT -> family.labelBase + " " + (idx + 1);
            case DIGITAL2TR -> (idx == 0 ? "2TR Digital L" : "2TR Digital R");
            case DIRECT -> family.labelBase + " " + (idx + 1);
            case GEQ -> family.labelBase + " " + (idx + 1);
            case BUSINSERT -> family.labelBase + " " + (idx + 1);
            case AUXINSERT -> family.labelBase + " " + (idx + 1);
            case STEREOINSERT -> (idx == 0 ? "Stereo Insert L" : "Stereo Insert R");
        };
    }
}
