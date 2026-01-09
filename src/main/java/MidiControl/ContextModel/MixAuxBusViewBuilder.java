package MidiControl.ContextModel;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MixAuxBusViewBuilder implements ViewBuilder {

    private final CanonicalRegistry registry;

    public MixAuxBusViewBuilder(CanonicalRegistry registry) {
        this.registry = registry;
    }

    @Override
    public List<ViewControl> build(Context context, CanonicalRegistry registry) {
        return buildCompact(context);
    }

    public List<ViewControl> buildCompact(Context context) {
        List<ViewControl> result = new ArrayList<>();

        if (!supports(context)) {
            return result;
        }

        List<ControlInstance> all =
                registry.getAllInstancesForContext(context.getId());

        String family = context.getId().split("\\.")[0];
        String prefix = switch (family) {
            case "mix" -> "kMix";
            case "aux" -> "kAUX";
            default -> "";
        };

        all.stream()
            .filter(ci -> (prefix + "Fader").equals(ci.getGroup()))
            .filter(ci -> "kFader".equals(ci.getSubcontrol()))
            .findFirst()
            .ifPresent(ci -> result.add(createFader(ci)));

        all.stream()
            .filter(ci ->
            (prefix + "Pan").equals(ci.getGroup()) || (prefix + "Balance").equals(ci.getGroup())
            )
            .findFirst()
            .ifPresent(ci -> result.add(createPan(ci)));

        all.stream()
            .filter(ci ->
                ci.getGroup().contains(prefix +"Comp") ||
                ci.getGroup().contains(prefix + "Dyn")
            )
            .filter(ci -> ci.getSubcontrol().endsWith("Threshold"))
            .sorted(Comparator.comparing(ControlInstance::getSubcontrol))
            .forEach(ci -> result.add(createDynamics(ci)));

        all.stream()
            .filter(ci -> ci.getGroup().startsWith(prefix + "EQ"))
            .filter(ci -> ci.getSubcontrol().endsWith("G"))
            .sorted(Comparator.comparing(ControlInstance::getSubcontrol))
            .forEach(ci -> result.add(createEQGain(ci)));

        return result;
    }

    public boolean supports(Context context) {
        return context.getId().startsWith("mix.") ||
               context.getId().startsWith("aux.");
    }

    private ViewControl createFader(ControlInstance ci) {
        return new ViewControl(
                "FADER",
                ci.getGroup(),
                "Fader",
                ControlType.FADER,
                0,
                ci.getMin(),
                ci.getMax(),
                ci.getValue(),
                ci.getSysex().getDefault_value(),
                ci.getGroup(),
                ci.getSubcontrol(),
                ci.getInstanceIndex()
        );
    }

    private ViewControl createPan(ControlInstance ci) {
        return new ViewControl(
                "PAN",
                ci.getGroup(),
                "Pan",
                ControlType.SLIDER_HORIZONTAL,
                0,
                ci.getMin(),
                ci.getMax(),
                ci.getValue(),
                ci.getSysex().getDefault_value(),
                ci.getGroup(),
                ci.getSubcontrol(),
                ci.getInstanceIndex()
        );
    }

    private ViewControl createDynamics(ControlInstance ci) {
        return new ViewControl(
                "DYN_" + ci.getSubcontrol(),
                ci.getGroup(),
                "Dyn Thresh",
                ControlType.KNOB,
                0,
                ci.getMin(),
                ci.getMax(),
                ci.getValue(),
                ci.getSysex().getDefault_value(),
                ci.getGroup(),
                ci.getSubcontrol(),
                ci.getInstanceIndex()
        );
    }

    private ViewControl createEQGain(ControlInstance ci) {
        return new ViewControl(
                "EQ_" + ci.getSubcontrol(),
                ci.getGroup(),
                "EQ Gain",
                ControlType.KNOB,
                0,
                ci.getMin(),
                ci.getMax(),
                ci.getValue(),
                ci.getSysex().getDefault_value(),
                ci.getGroup(),
                ci.getSubcontrol(),
                ci.getInstanceIndex()
        );
    }
}