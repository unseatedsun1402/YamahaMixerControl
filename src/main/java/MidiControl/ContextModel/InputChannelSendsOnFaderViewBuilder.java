package MidiControl.ContextModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        ControlInstance toggle =
                registry.find(channelIndex,
                            "kInputOn",
                            "kChannelOn");

        if (toggle != null) {
            result.add(createToggle(toggle, viewType, viewSuffix));
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
            result.add(createPan(pan, viewType, viewSuffix));
        }

        // EQ1

        ControlInstance eq1 =
                registry.find(channelIndex,
                            "kInputEQ",
                            "kEQ1G");

        if (eq1 == null) {
            eq1 = registry.find(channelIndex,
                                "kInputEQ",
                                "kEQLowG");
        }

        if (eq1 != null) {
            result.add(createEQGain(eq1, viewType, viewSuffix, 1));
        }

        // EQ2

        ControlInstance eq2 =
                registry.find(channelIndex,
                            "kInputEQ",
                            "kEQ2G");

        if (eq2 == null) {
            eq2 = registry.find(channelIndex,
                                "kInputEQ",
                                "kEQLowMidG");
        }

        if (eq2 != null) {
            result.add(createEQGain(eq2, viewType, viewSuffix, 2));
        }

        // EQ3

        ControlInstance eq3 =
                registry.find(channelIndex,
                            "kInputEQ",
                            "kEQ3G");

        if (eq3 == null) {
            eq3 = registry.find(channelIndex,
                                "kInputEQ",
                                "kEQHiMidG");
        }

        if (eq3 != null) {
            result.add(createEQGain(eq3, viewType, viewSuffix, 3));
        }

        // EQ4

        ControlInstance eq4 =
                registry.find(channelIndex,
                            "kInputEQ",
                            "kEQ4G");

        if (eq4 == null) {
            eq4 = registry.find(channelIndex,
                                "kInputEQ",
                                "kEQHiG");
        }

        if (eq4 != null) {
            result.add(createEQGain(eq4, viewType, viewSuffix, 4));
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
                        ci -> result.add(createSendAsFader(ci, viewType, viewSuffix)),
                        () -> {
                            if(! targetBusId.contains("EDIT")) logger.warning("SOF: No matching send found for " + targetBusId);
                        }
                );

        return result;
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

    private int extractSendIndex(String subcontrol) {
        Matcher m = SEND_PATTERN.matcher(subcontrol);
        if (!m.matches()) return -1;
        return Integer.parseInt(m.group(2));
    }

    private ViewControl createToggle(ControlInstance ci, String viewType, String viewSuffix) {
        return new ViewControl(
                "CHANNEL_ON",
                "input.control",
                "On",
                ControlType.TOGGLE,
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
                "INPUT_CHANNEL_ON",
                null,
                ci.getInstanceIndex()
        );
    }

    private ViewControl createPan(ControlInstance ci, String viewType, String viewSuffix) {
        return new ViewControl(
                "PAN",
                "input.pan",
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
                "INPUT_PAN",
                null,
                ci.getInstanceIndex()
        );
    }

        private ViewControl createEQGain(ControlInstance ci, String viewType, String viewSuffix, int subControlindex) {
        return new ViewControl(
                String.format("EQ%dG",subControlindex),
                String.format("input.eq%dg",subControlindex),
                String.format("EQ %d Gain",subControlindex),
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
                String.format("INPUT_EQ%d_GAIN",subControlindex),
                null,
                ci.getInstanceIndex()
        );
    }

    private ViewControl createSendAsFader(ControlInstance ci, String viewType, String viewSuffix) {

        int sendIndex = extractSendIndex(ci.getSubcontrol());
        String logicId = "SEND_" + targetBusId;

        return new ViewControl(
                logicId,
                "input.send.sof",
                targetBusId,
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
                "INPUT_SEND_LEVEL",
                sendIndex,
                ci.getInstanceIndex()
        );
    }
}