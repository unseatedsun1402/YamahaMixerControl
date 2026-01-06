package MidiControl.ContextModel;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Compact Mix/Aux Bus ViewBuilder
 *
 * Includes ONLY:
 *  - FADER          (kMixFader / kAUXFader)
 *  - PAN            (kMixPan / kAUXPan)
 *  - DYNAMICS       (kMixDyn* / kAUXDyn*)
 *  - 4‑KNOB EQ      (Gain controls only: kMixEQ*Gain / kAUXEQ*Gain)
 *
 * Everything else is intentionally ignored.
 */
public class MixAuxBusViewBuilder implements ViewBuilder {

    private final CanonicalRegistry registry;

    public MixAuxBusViewBuilder(CanonicalRegistry registry) {
        this.registry = registry;
    }

    @Override
    public List<ViewControl> build(Context context, CanonicalRegistry registry) {
        return buildCompact(context);
    }

    // -------------------------------------------------------------------------
    // MAIN BUILD METHOD
    // -------------------------------------------------------------------------

    public List<ViewControl> buildCompact(Context context) {
        List<ViewControl> result = new ArrayList<>();

        // Only handle mix.* and aux.* contexts
        if (!supports(context)) {
            return result;
        }

        List<ControlInstance> all =
                registry.getAllInstancesForContext(context.getId());

        // Determine prefix: kMix or kAUX
        String family = context.getId().split("\\.")[0]; // mix or aux
        String prefix = "k" + family.substring(0,1).toUpperCase() + family.substring(1);

        // 1. FADER
        all.stream()
                .filter(ci -> (prefix + "Fader").equals(ci.getGroup()))
                .filter(ci -> "kFader".equals(ci.getSubcontrol()))
                .findFirst()
                .ifPresent(ci -> result.add(createFader(ci)));

        // 2. PAN (to stereo)
        all.stream()
                .filter(ci -> (prefix + "Pan").equals(ci.getGroup()))
                .findFirst()
                .ifPresent(ci -> result.add(createPan(ci)));

        // 3. DYNAMICS (all dyn controls)
        all.stream()
                .filter(ci -> ci.getGroup().startsWith(prefix + "Dyn"))
                .sorted(Comparator.comparing(ci -> ci.getSubcontrol()))
                .forEach(ci -> result.add(createDynamics(ci)));

        // 4. 4‑KNOB EQ (gain controls only)
        all.stream()
                .filter(ci -> ci.getGroup().startsWith(prefix + "EQ"))
                .filter(ci -> ci.getSubcontrol().contains("Gain"))
                .sorted(Comparator.comparing(ci -> ci.getSubcontrol()))
                .forEach(ci -> result.add(createEQGain(ci)));

        return result;
    }

    // -------------------------------------------------------------------------
    // SUPPORTS
    // -------------------------------------------------------------------------

    public boolean supports(Context context) {
        return context.getId().startsWith("mix.") ||
               context.getId().startsWith("aux.");
    }

    // -------------------------------------------------------------------------
    // CONTROL FACTORIES
    // -------------------------------------------------------------------------

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
                "Dyn",
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
                "EQ",
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