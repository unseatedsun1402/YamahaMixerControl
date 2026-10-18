package MidiControl.ContextModel.ViewBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import MidiControl.ContextModel.Context;
import MidiControl.ContextModel.ContextFilter;
import MidiControl.ContextModel.ControlType;
import MidiControl.ContextModel.ViewBuilder;
import MidiControl.ContextModel.ViewControl;
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
                    MidiControl.Controls.Widgets.Fader.createBusFader(fader,viewType,viewSuffix));
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


        addEqControls(result, all, prefix, viewType, viewSuffix);

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

    private void addEqControls(
            List<ViewControl> result,
            List<ControlInstance> controls,
            String prefix,
            String viewType,
            String viewSuffix
    ) {
        String[] legacyBandNames = {"kEQLow", "kEQLowMid", "kEQHiMid", "kEQHi"};

        for (int band = 0; band < legacyBandNames.length; band++) {
            int bandNumber = band + 1;
            String canonicalPrefix = "kEQ" + bandNumber;
            String legacyPrefix = legacyBandNames[band];

            for (char parameter : new char[] {'G', 'F', 'Q'}) {
                ControlInstance control = findFirst(
                        controls,
                        prefix + "EQ",
                        canonicalPrefix + parameter,
                        "kAUXEQ",
                        legacyPrefix + parameter);

                if (control != null) {
                    result.add(MidiControl.Controls.Widgets.EQ.createEqBandControl(
                            control,
                            bandNumber,
                            parameter,
                            viewType,
                            viewSuffix));
                }
            }
        }
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
