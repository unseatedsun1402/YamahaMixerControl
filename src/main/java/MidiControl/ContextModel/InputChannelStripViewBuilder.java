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
        return buildCompact(context, registry);
    }

    public List<ViewControl> buildCompact(Context context, CanonicalRegistry registry) {
        List<ViewControl> result = new ArrayList<>();

        int instanceIndex = extractContextIndex(context.getId());
        if (instanceIndex < 0)
            return result;

        List<ControlInstance> all = registry.getAllInstancesForContext(context.getId());
        // 1. MIX SEND
        all.stream()
                .filter(ci ->
                        ci.getGroup().equals("kInputToMix") ||
                        ci.getGroup().equals("kInputAUX")      // 01V96i AUX sends
                )
                .filter(ci -> isSendLevel(ci.getSubcontrol()))
                .sorted(Comparator.comparingInt(ci -> extractSendIndex(ci.getSubcontrol())))
                .forEach(ci -> result.add(createSendMix(ci)));

        // 2. FADER
        all.stream()
                .filter(ci -> "kInputFader".equals(ci.getGroup()))
                .filter(ci -> "kFader".equals(ci.getSubcontrol()))
                .findFirst()
                .ifPresent(ci -> result.add(createFader(ci)));

        return result;
    }

    private ViewControl createSendMix(ControlInstance ci) {
        int mixIndex = extractSendIndex(ci.getSubcontrol());
        String logicId = "SEND_MIX" + mixIndex;
        String label = "Mix " + mixIndex;

        return new ViewControl(
                logicId,
                "kInputToMix",   // canonical UI group
                label,
                ControlType.KNOB,
                mixIndex - 1,
                ci.getMin(),
                ci.getMax(),
                ci.getValue(),
                ci.getSysex().getDefault_value(),
                ci.getGroup(),
                ci.getSubcontrol(),
                ci.getInstanceIndex()
        );
    }

    private ViewControl createFader(ControlInstance ci) {
        return new ViewControl(
                "FADER",
                "kInputFader",
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