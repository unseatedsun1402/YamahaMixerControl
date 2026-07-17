package MidiControl.ContextModel;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;

public class MixAuxBusViewBuilder implements ViewBuilder {
    private static Logger logger = Logger.getLogger(MixAuxBusViewBuilder.class.getName());

    public MixAuxBusViewBuilder() {}

    @Override
    public List<ViewControl> build(
            Context context,
            CanonicalRegistry registry,
            String suffix) {

        List<ViewControl> result =
                new ArrayList<>();

        if (!supports(context)) {
            return result;
        }

        String contextId =
                context.getId();

        String family =
                contextId.split("\\.")[0];

        String prefix =
                switch (family) {
                    case "mix" -> "kMix";
                    case "aux" -> "kAUX";
                    case "stereo" -> "kStereo";
                    default -> null;
                };

                
        logger.info(
            "Building " + contextId
        );


        if (prefix == null) {
            return result;
        }

        String viewType =
                "mix-bus-view";

        String viewSuffix =
                suffix != null
                        ? suffix
                        : contextId;
        
        
        int busIndex =
            registry.extractContextIndex(
                context.getId()
            );


        ControlInstance fader =
            registry.find(
                busIndex,
                prefix + "Fader",
                "kFader");

        boolean stereo = context.getId().startsWith("stereo.");      
        
        if(stereo){
            logger.info("Found a stereo fader");
            fader = registry.find(
                    0,
                    "kStereoFader",
                    "kFader");
            if(fader == null)logger.warning("Stereo fader not found despite context hit");
        }


        if (fader != null) {
            result.add(
                    createFader(
                            fader,
                            viewType,
                            viewSuffix));
        }

        ControlInstance pan =
                registry.find(
                        busIndex,
                        prefix + "Pan",
                        "kPan");

        if (pan == null) {
            pan = registry.find(
                    busIndex,
                    prefix + "Pan",
                    "kBalance");
        }

        if (pan != null) {
            result.add(createControl(
                    pan,
                    "PAN",
                    "bus.pan",
                    "Pan",
                    ControlType.SLIDER_HORIZONTAL,
                    viewType,
                    viewSuffix,
                    "BUS_PAN",
                    null));
        }

        ControlInstance dynOn =
                registry.find(
                        busIndex,
                        prefix + "Comp",
                        "kCompOn");

        if (dynOn == null) {
            dynOn = registry.find(
                    busIndex,
                    prefix + "Dyn",
                    "kDynaOn");
        }

        ControlInstance ratio =
                registry.find(
                        busIndex,
                        prefix + "Comp",
                        "kCompRatio");

        if (ratio == null) {
            ratio = registry.find(
                    busIndex,
                    prefix + "Dyn",
                    "kDynaRatio");
        }

        ControlInstance threshold =
                registry.find(
                        busIndex,
                        prefix + "Comp",
                        "kCompThreshold");

        if (threshold == null) {
            threshold = registry.find(
                    busIndex,
                    prefix + "Dyn",
                    "kDynaThreshold");
        }

        if (dynOn != null) {
            result.add(createControl(
                    dynOn,
                    "DYN2_ON",
                    "input.dynamics",
                    "DYN2 On",
                    ControlType.TOGGLE,
                    viewType,
                    viewSuffix,
                    "DYNAMICS2_ON",
                    null));
        }

        if (ratio != null) {
            result.add(createControl(
                ratio,
                "DYN2_RATIO",
                "input.dynamics",
                "DYN2 Ratio",
                ControlType.SLIDER_HORIZONTAL,
                viewType,
                viewSuffix,
                "DYNAMICS2_RATIO",
                null));
        }

        if (threshold != null) {
            result.add(createControl(
                    threshold,
                    "THRESHOLD",
                    "input.dynamics",
                    "Threshold",
                    ControlType.SLIDER_HORIZONTAL,
                    viewType,
                    viewSuffix,
                    "DYNAMICS2_THRESHOLD",
                    null));
        }

        ControlInstance eq1 =
                registry.find(
                        busIndex,
                        prefix + "EQ",
                        "kEQ1G");
        
        
        if (eq1 == null) {
            eq1 = registry.find(
                    busIndex,
                    "kAUXEQ",
                    "kEQLowG");
        }


        ControlInstance eq2 =
                registry.find(
                        busIndex,
                        prefix + "EQ",
                        "kEQ2G");

        if (eq2 == null) {
            eq2 = registry.find(
                    busIndex,
                    "kAUXEQ",
                    "kEQLowMidQ");
        }

        ControlInstance eq3 =
                registry.find(
                        busIndex,
                        prefix + "EQ",
                        "kEQ3G");

        if (eq3 == null) {
            eq3 = registry.find(
                busIndex,
                "kAUXEQ",
                "kEQHiMidG");
        }

        ControlInstance eq4 =
                registry.find(
                        busIndex,
                        prefix + "EQ",
                        "kEQ4G");

        if (eq4 == null) {
            eq4 = registry.find(
                busIndex,
                "kAUXEQ",
                "kEQHiQ");
        }

        if (eq1 != null) {
            result.add(createControl(
                    eq1,
                    "EQ1G",
                    "input.eq",
                    "EQ 1 Gain",
                    ControlType.SLIDER_HORIZONTAL,
                    viewType,
                    viewSuffix,
                    "INPUT_EQ1_GAIN",
                    null));
        }

        if (eq2 != null) {
            result.add(createControl(
                    eq2,
                    "EQ2G",
                    "input.eq",
                    "EQ 2 Gain",
                    ControlType.SLIDER_HORIZONTAL,
                    viewType,
                    viewSuffix,
                    "INPUT_EQ2_GAIN",
                    null));
        }

        if (eq3 != null) {
            result.add(createControl(
                    eq3,
                    "EQ3G",
                    "input.eq",
                    "EQ 3 Gain",
                    ControlType.SLIDER_HORIZONTAL,
                    viewType,
                    viewSuffix,
                    "INPUT_EQ3_GAIN",
                    null));
        }

        if (eq4 != null) {
            result.add(createControl(
                    eq4,
                    "EQ4G",
                    "input.eq",
                    "EQ 4 Gain",
                    ControlType.SLIDER_HORIZONTAL,
                    viewType,
                    viewSuffix,
                    "INPUT_EQ4_GAIN",
                    null));
        }

        return result;
    }


    public boolean supports(Context context) {
        return context.getId().startsWith("mix.")
            || context.getId().startsWith("aux.")
            || context.getId().startsWith("stereo.");
    }


    private ViewControl createFader(
            ControlInstance ci,
            String viewType,
            String viewSuffix) {

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

    private ViewControl createControl(
            ControlInstance ci,
            String logicalId,
            String uiGroup,
            String label,
            ControlType type,
            String viewType,
            String viewSuffix,
            String role,
            Integer sendIndex) {

        return new ViewControl(
                logicalId,
                uiGroup,
                label,
                type,
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
                role,
                sendIndex,
                ci.getInstanceIndex()
        );
    }
}