package MidiControl.ContextModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;

public class MixAuxBusViewBuilder implements ViewBuilder {

    public MixAuxBusViewBuilder() {}

    @Override
    public List<ViewControl> build(Context context, CanonicalRegistry registry, String suffix) {
        return buildCompact(context, registry, suffix);
    }

    
    public boolean supports(Context context) {
        return context.getId().startsWith("mix.") ||
               context.getId().startsWith("aux.");
    }

    public List<ViewControl> buildCompact(Context context, CanonicalRegistry registry, String suffix) {

        List<ViewControl> result = new ArrayList<>();

        if (!supports(context)) {
            return result;
        }

        List<ControlInstance> all =
                registry.getAllInstancesForContext(context.getId());

        String contextId = context.getId();
        String family = contextId.split("\\.")[0];

        String prefix = switch (family) {
            case "mix" -> "kMix";
            case "aux" -> "kAUX";
            default -> null;
        };

        if (prefix == null) return result;

        String viewType = "mix-bus-view";
        String viewSuffix = suffix != null ? suffix : contextId;

        // ------------------------------------------------------------
        // FADER (kMixFader / kAUXFader)
        // ------------------------------------------------------------
        all.stream()
            .filter(ci ->
                (ci.getGroup().equals("kMixFader") || ci.getGroup().equals("kAUXFader")) &&
                ci.getSubcontrol().equals("kFader")
            )
            .findFirst()
            .ifPresent(ci -> result.add(createFader(ci, contextId, viewType, viewSuffix)));

        // ------------------------------------------------------------
        // PAN / BALANCE
        // ------------------------------------------------------------
        all.stream()
            .filter(ci ->
                (ci.getGroup().equals("kMixPan") || ci.getGroup().equals("kAUXPan"))
            )
            .filter(ci ->
                ci.getSubcontrol().equals("kPan") ||
                ci.getSubcontrol().equals("kBalance")
            )
            .findFirst()
            .ifPresent(ci -> result.add(createPan(ci, contextId, viewType, viewSuffix)));

        // ------------------------------------------------------------
        // DYNAMICS
        // ------------------------------------------------------------
        all.stream()
            .filter(ci ->
                (ci.getGroup().equals("kMixComp") || ci.getGroup().equals("kMixDyn") ||
                 ci.getGroup().equals("kAUXComp") || ci.getGroup().equals("kAUXDyn"))
            )
            .filter(ci -> ci.getSubcontrol().endsWith("Threshold"))
            .sorted(Comparator.comparingInt(ci -> extractIndex(ci.getSubcontrol())))
            .forEach(ci -> result.add(createDynamics(ci, contextId, viewType, viewSuffix)));

        // ------------------------------------------------------------
        // EQ
        // ------------------------------------------------------------
        all.stream()
            .filter(ci ->
                ci.getGroup().startsWith("kMixEQ") ||
                ci.getGroup().startsWith("kAUXEQ")
            )
            .filter(ci -> ci.getSubcontrol().endsWith("G"))
            .sorted(Comparator.comparingInt(ci -> extractIndex(ci.getSubcontrol())))
            .forEach(ci -> result.add(createEQGain(ci, contextId, viewType, viewSuffix)));

        return result;
    }

    private ViewControl createFader(ControlInstance ci, String contextId, String viewType, String viewSuffix) {
        return new ViewControl(
                "FADER",
                "bus.fader",
                "Fader",
                ControlType.FADER,
                0,
                ci.getMin(),
                ci.getMax(),
                ci.getValue(),
                ci.getSysex().getDefault_value(),
                ci.getGroup(),
                ci.getSubcontrol(),
                ci.getInstanceIndex(),
                viewType,
                viewSuffix,
                "BUS_FADER",
                null,
                ci.getInstanceIndex()
        );
    }

    private ViewControl createPan(ControlInstance ci, String contextId, String viewType, String viewSuffix) {
        return new ViewControl(
                "PAN",
                "bus.pan",
                "Pan",
                ControlType.SLIDER_HORIZONTAL,
                0,
                ci.getMin(),
                ci.getMax(),
                ci.getValue(),
                ci.getSysex().getDefault_value(),
                ci.getGroup(),
                ci.getSubcontrol(),
                ci.getInstanceIndex(),
                viewType,
                viewSuffix,
                "BUS_PAN",
                null,
                ci.getInstanceIndex()
        );
    }

    private ViewControl createDynamics(ControlInstance ci, String contextId, String viewType, String viewSuffix) {
        return new ViewControl(
                "DYN_" + ci.getSubcontrol(),
                "bus.dynamics",
                "Dyn Thresh",
                ControlType.KNOB,
                0,
                ci.getMin(),
                ci.getMax(),
                ci.getValue(),
                ci.getSysex().getDefault_value(),
                ci.getGroup(),
                ci.getSubcontrol(),
                ci.getInstanceIndex(),
                viewType,
                viewSuffix,
                "BUS_DYNAMICS",
                null,
                ci.getInstanceIndex()
        );
    }

    private ViewControl createEQGain(ControlInstance ci, String contextId, String viewType, String viewSuffix) {
        return new ViewControl(
                "EQ_" + ci.getSubcontrol(),
                "bus.eq",
                "EQ Gain",
                ControlType.KNOB,
                0,
                ci.getMin(),
                ci.getMax(),
                ci.getValue(),
                ci.getSysex().getDefault_value(),
                ci.getGroup(),
                ci.getSubcontrol(),
                ci.getInstanceIndex(),
                viewType,
                viewSuffix,
                "BUS_EQ_GAIN",
                null,
                ci.getInstanceIndex()
        );
    }

    private int extractIndex(String subcontrol) {
        String digits = subcontrol.replaceAll("\\D+", "");
        if (digits.isEmpty()) return Integer.MAX_VALUE;
        return Integer.parseInt(digits);
    }
}