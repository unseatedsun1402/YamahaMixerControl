package MidiControl.ContextModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;

/**
 * Compact Input Channel Strip ViewBuilder
 *
 * Includes ONLY:
 *  - SEND_MIX{n}     (kInputToMix.kMix{n}Level or kInputAUX.kAUX{n}Level)
 *  - FADER           (kInputFader.kFader)
 */
public class InputChannelStripViewBuilder implements ViewBuilder {

    public InputChannelStripViewBuilder() {}

    @Override
    public List<ViewControl> build(Context context, CanonicalRegistry registry, String suffix) {
        return buildCompact(context, registry, suffix);
    }

    public List<ViewControl> buildCompact(Context context, CanonicalRegistry registry, String suffix) {

        List<ViewControl> result = new ArrayList<>();

        int channelIndex = extractContextIndex(context.getId());
        if (channelIndex < 0) return result;

        String viewType = "input-channel-view";
        String viewSuffix = suffix != null ? suffix : context.getId();

        List<ControlInstance> all =
                registry.getAllInstancesForContext(context.getId());

        all.stream()
            .filter(ci ->
                ci.getGroup().equals("kInputToMix") ||
                ci.getGroup().equals("kInputAUX")
            )
            .filter(ci -> isSendLevel(ci.getSubcontrol()))
            .sorted(Comparator.comparingInt(ci -> extractSendIndex(ci.getSubcontrol())))
            .forEach(ci ->
                result.add(createSendMix(ci, viewType, viewSuffix, channelIndex))
            );

        all.stream()
            .filter(ci -> ci.getGroup().equals("kInputFader"))
            .filter(ci -> ci.getSubcontrol().equals("kFader"))
            .findFirst()
            .ifPresent(ci ->
                result.add(createFader(ci, viewType, viewSuffix, channelIndex))
            );

        return result;
    }

    private ViewControl createSendMix(
            ControlInstance ci,
            String viewType,
            String viewSuffix,
            int channelIndex
    ) {
        int mixIndex = extractSendIndex(ci.getSubcontrol());

        String logicId = "SEND_MIX" + mixIndex;
        String label = "Mix " + mixIndex;

        return new ViewControl(
                logicId,
                "input.send",
                label,
                ControlType.KNOB,
                mixIndex - 1,
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
                mixIndex,
                channelIndex
        );
    }

    private ViewControl createFader(
            ControlInstance ci,
            String viewType,
            String viewSuffix,
            int channelIndex
    ) {
        return new ViewControl(
                "FADER",
                "input.fader",
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
                "INPUT_FADER",
                null,
                channelIndex
        );
    }

    private int extractContextIndex(String contextId) {
        int dot = contextId.lastIndexOf('.');
        if (dot == -1)
            return -1;

        try {
            return Integer.parseInt(contextId.substring(dot + 1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // Only the real patterns we know:
    // - kMix1Level  (M7CL/LS9/QL/CL)
    // - kAUX1Level  (01V96i)
    private static final Pattern SEND_PATTERN =
            Pattern.compile("^k(Mix|AUX)(\\d+)Level$");

    private boolean isSendLevel(String sub) {
        return SEND_PATTERN.matcher(sub).matches();
    }

    private int extractSendIndex(String sub) {
        Matcher m = SEND_PATTERN.matcher(sub);
        if (m.matches()) {
            return Integer.parseInt(m.group(2));
        }
        return Integer.MAX_VALUE;
    }
}