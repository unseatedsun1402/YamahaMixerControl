package MidiControl.ContextModel.ViewBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import MidiControl.ContextModel.Context;
import MidiControl.ContextModel.ControlType;
import MidiControl.ContextModel.ViewBuilder;
import MidiControl.ContextModel.ViewControl;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;

public class InputChannelSendsOnFaderViewBuilder implements ViewBuilder {

    private static final Logger logger =
            Logger.getLogger(InputChannelSendsOnFaderViewBuilder.class.getName());

    private static final Pattern SEND_PATTERN =
            Pattern.compile("^k(Mix|AUX)(0*[0-9]+)Level$", Pattern.CASE_INSENSITIVE);

    private String targetBusId = "MIX1";
    private boolean hasMixBuses = false;
    private boolean hasAuxBuses = false;

    public InputChannelSendsOnFaderViewBuilder() {}

    @Override
    public List<ViewControl> build(Context context,
                                CanonicalRegistry registry,
                                String suffix) {

        
        int channelIndex =registry.extractContextIndex(context.getId());

        if (suffix != null && !suffix.isBlank()) {
            targetBusId = suffix.toUpperCase();
        }

        String viewType = "input-sof-view";
        String viewSuffix = suffix != null ? suffix : targetBusId;

        List<ViewControl> result = new ArrayList<>();

        // CHANNEL ON

        ControlInstance channelOn =
                registry.find(channelIndex,
                            "kInputOn",
                            "kChannelOn");
        
        if (channelOn == null) {
            channelOn = registry.find(channelIndex,
                                "kInputChannelOn",
                                "kChannelOn");
        }

        if (channelOn != null) {
            result.add(MidiControl.Controls.Widgets.ChannelOn.createChannelOn(
                    channelOn,
                    viewType,
                    viewSuffix,
                    channelIndex));
        }

        // PAN

        ControlInstance pan =
                registry.find(channelIndex,
                            "kInputPan",
                            "kChannelPan");

        if (pan == null) {
            pan = registry.find(channelIndex,
                                "kInputChannelPan",
                                "kChannelPan");
        }

        if (pan != null) {
            result.add(MidiControl.Controls.Widgets.Pan.createPan(pan, viewType, viewSuffix, channelIndex));
        }

        addEqControls(result, registry, channelIndex, viewType, viewSuffix);

        // GATE/DYN1

        ControlInstance dyn1On = registry.find(channelIndex,
                    "kInputDynamics1",
                    "kDynaOn");
        
        if (dyn1On == null) {
            dyn1On = registry.find(channelIndex,
                                "kInputGate",
                                "kGateOn");
        }

        if (dyn1On != null) {
            result.add(MidiControl.Controls.Widgets.Generic.createGenericControl(
                    dyn1On,
                    "DYN1_ON",
                    "input.dynamics",
                    "DYN1 On",
                    ControlType.TOGGLE,
                    viewType,
                    viewSuffix,
                    "DYNAMICS1_ON",
                    null));
        }

        // COMP/DYN2

        ControlInstance dyn2On = registry.find(channelIndex,
                    "kInputDynamics2",
                    "kDynaOn");
        
        ControlInstance dyn2Ratio = registry.find(channelIndex,
            "kInputDynamics2", 
            "kDynaRatio");
        
        ControlInstance dyn2Thresh = registry.find(channelIndex, 
                "kInputDynamics2", 
                "kDynaThreshold");
        
        if (dyn2On == null) {
            dyn2On = registry.find(channelIndex,
                                "kInputComp",
                                "kCompOn");

            dyn2Ratio = registry.find(channelIndex, 
                "kInputComp", 
                "kCompRatio");

            dyn2Thresh = registry.find(channelIndex, 
                "kInputComp", 
                "kCompThreshold");
        }

        if (dyn2On != null) {
            result.add(MidiControl.Controls.Widgets.Generic.createGenericControl(
                dyn2On,
                "DYN2_ON",
                "input.dynamics",
                "DYN2 On",
                ControlType.TOGGLE,
                viewType,
                viewSuffix,
                "DYNAMICS2_ON",
                null));
        }

        if (dyn2Ratio != null){
            result.add(MidiControl.Controls.Widgets.Generic.createGenericControl(
                dyn2Ratio,
                "DYN2_RATIO",
                "input.dynamics",
                "DYN2 Ratio",
                ControlType.SLIDER_HORIZONTAL,
                viewType,
                viewSuffix,
                "DYNAMICS2_RATIO",
                null));
        }

        if (dyn2Thresh != null){
            result.add(MidiControl.Controls.Widgets.Generic.createGenericControl(
                    dyn2Thresh,
                "THRESHOLD",
                "input.dynamics",
                "Threshold",
                ControlType.SLIDER_HORIZONTAL,
                viewType,
                viewSuffix,
                "DYNAMICS2_THRESHOLD",
                null));
        }

        
        List<ControlInstance> all = registry.getAllInstancesForContext(context.getId());

        hasMixBuses = all.stream().anyMatch(ci -> ci.getSubcontrol().startsWith("kMix"));
        hasAuxBuses = all.stream().anyMatch(ci -> ci.getSubcontrol().startsWith("kAUX"));

        if (suffix != null && !suffix.isBlank()) {
            targetBusId = suffix.toUpperCase();
        }

        if (targetBusId.startsWith("MIX") && !hasMixBuses && hasAuxBuses) {
            targetBusId = targetBusId.replace("MIX", "AUX");
        }

        if (targetBusId.startsWith("AUX") && !hasAuxBuses && hasMixBuses) {
            targetBusId = targetBusId.replace("AUX", "MIX");
        }

        all.stream()
                .filter(ci ->
                        "kInputToMix".equals(ci.getGroup()) ||
                        "kInputAUX".equals(ci.getGroup()))
                .filter(ci -> isSendLevel(ci.getSubcontrol()))
                .filter(ci -> sendMatchesTargetBus(ci.getSubcontrol()))
                .findFirst()
                .ifPresentOrElse(
                        ci -> result.add(MidiControl.Controls.Widgets.Fader.createSendAsFader(ci, viewType, viewSuffix, targetBusId)),
                        () -> {
                            if(! targetBusId.contains("EDIT")) logger.warning("SOF: No matching send found for " + targetBusId);
                        }
                );

        return result;
    }

    private void addEqControls(
            List<ViewControl> result,
            CanonicalRegistry registry,
            int channelIndex,
            String viewType,
            String viewSuffix
    ) {
        String[] legacyBandNames = {"kEQLow", "kEQLowMid", "kEQHiMid", "kEQHi"};

        for (int band = 0; band < legacyBandNames.length; band++) {
            int bandNumber = band + 1;
            String canonicalPrefix = "kEQ" + bandNumber;
            String legacyPrefix = legacyBandNames[band];

            for (char parameter : new char[] {'G', 'F', 'Q'}) {
                ControlInstance control = registry.find(
                        channelIndex,
                        "kInputEQ",
                        canonicalPrefix + parameter);

                if (control == null) {
                    control = registry.find(
                            channelIndex,
                            "kInputEQ",
                            legacyPrefix + parameter);
                }

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


    private boolean isSendLevel(String sub) {
        return SEND_PATTERN.matcher(sub).matches();
    }

    private boolean sendMatchesTargetBus(String subcontrol) {
        Matcher m = SEND_PATTERN.matcher(subcontrol);
        if (!m.matches()) return false;

        String type = m.group(1).toUpperCase();
        String indexRaw = m.group(2);
        int index = Integer.parseInt(indexRaw);

        String busId = type + index;

        return busId.equalsIgnoreCase(targetBusId);
    }
}
