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
        
        
        List<ControlInstance> all =
                new ArrayList<>();

        for (ContextFilter filter : context.getFilters()) {
        all.addAll(
                registry.getInstancesForFilter(
                        filter));
        }


        ControlInstance fader =findInstance(all,prefix + "Fader","kFader");
        
        if (context.getId().startsWith("stereo.")) {
        fader = findInstance(
                all,
                "kStereoFader",
                "kFader");
        }

        if (fader != null) {
            result.add(
                    createFader(
                            fader,
                            viewType,
                            viewSuffix));
        }

        ControlInstance pan =findFirst(all,prefix + "Pan","kPan",prefix + "Pan","kBalance");

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
                findFirst(
                        all,
                        prefix + "Comp",
                        "kCompOn",
                        prefix + "Dyn",
                        "kDynaOn");

        ControlInstance ratio =
                findFirst(
                        all,
                        prefix + "Comp",
                        "kCompRatio",
                        prefix + "Dyn",
                        "kDynaRatio");

        ControlInstance threshold =
                findFirst(
                        all,
                        prefix + "Comp",
                        "kCompThreshold",
                        prefix + "Dyn",
                        "kDynaThreshold");


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


        ControlInstance eq1 =
                findFirst(
                        all,
                        prefix + "EQ",
                        "kEQ1G",
                        "kAUXEQ",
                        "kEQLowG");

        ControlInstance eq2 =
                findFirst(
                        all,
                        prefix + "EQ",
                        "kEQ2G",
                        "kAUXEQ",
                        "kEQLowMidQ");

        ControlInstance eq3 =
                findFirst(
                        all,
                        prefix + "EQ",
                        "kEQ3G",
                        "kAUXEQ",
                        "kEQHiMidG");

        ControlInstance eq4 =
                findFirst(
                        all,
                        prefix + "EQ",
                        "kEQ4G",
                        "kAUXEQ",
                        "kEQHiQ");

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

        System.out.println("controls = " + result.size());

        for (ViewControl c : result) {
        System.out.println(c.logicId);
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

    private ViewControl createControl( ControlInstance ci,String logicalId,String uiGroup,String label,
            ControlType type,String viewType,String viewSuffix,String role,Integer sendIndex) {

        return new ViewControl(logicalId,uiGroup,label,type,0,ci.getMin(),ci.getMax(),ci.getValue(),ci.getSysex().getDefault_value(),
                ci.getGroup(),ci.getSubcontrol(),ci.getInstanceIndex(),viewType,viewSuffix,role,sendIndex,ci.getInstanceIndex()
        );
    }

    private ControlInstance findInstance(
        List<ControlInstance> instances,
        String group,
        String subcontrol) {

        return instances.stream()
                .filter(ci -> group.equals(ci.getGroup()) && subcontrol.equals(ci.getSubcontrol()))
                .findFirst()
                .orElse(null);
        }

        private ControlInstance findFirst(
                List<ControlInstance> instances,
                String group1,
                String sub1,
                String group2,
                String sub2) {

                ControlInstance result =
                        findInstance(
                                instances,
                                group1,
                                sub1);

                if (result != null) {
                return result;
                }

                return findInstance(
                        instances,
                        group2,
                        sub2);
        }
}
