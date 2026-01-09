package MidiControl.ContextModel;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compact Input Channel Strip ViewBuilder
 *
 * Includes ONLY:
 *  - CHANNEL_ON      (kInputOn.kChannelOn)
 *  - PAN             (kInputPan.kChannelPan)
 *  - SEND_MIX{n}     (kInputToMix.kMix{n}Level or kInputAUX.kAUX{n}Level)
 *  - FADER           (kInputFader.kFader)
 *
 * Everything else is intentionally ignored.
 */
public class InputChannelStripViewBuilder implements ViewBuilder {

    private final CanonicalRegistry registry;

    public InputChannelStripViewBuilder(CanonicalRegistry registry) {
        this.registry = registry;
    }

    @Override
    public List<ViewControl> build(Context context, CanonicalRegistry registry) {
        return buildCompact(context);
    }

    public List<ViewControl> buildCompact(Context context) {
        List<ViewControl> result = new ArrayList<>();

        int instanceIndex = extractContextIndex(context.getId());
        if (instanceIndex < 0)
            return result;

        List<ControlInstance> all = registry.getAllInstancesForContext(context.getId());

        // 1. CHANNEL_ON
        all.stream()
                .filter(ci -> "kInputOn".equals(ci.getGroup()))
                .filter(ci -> "kChannelOn".equals(ci.getSubcontrol()))
                .findFirst()
                .ifPresent(ci -> result.add(createChannelOn(ci)));

        // 2. PAN
        all.stream()
                .filter(ci -> "kInputPan".equals(ci.getGroup()))
                .filter(ci -> "kChannelPan".equals(ci.getSubcontrol()))
                .findFirst()
                .ifPresent(ci -> result.add(createPan(ci)));

        // 3. SEND_MIX{n}
        all.stream()
                .filter(ci ->
                        ci.getGroup().equals("kInputToMix") ||
                        ci.getGroup().equals("kInputAUX")      // 01V96i AUX sends
                )
                .filter(ci -> isSendLevel(ci.getSubcontrol()))
                .sorted(Comparator.comparingInt(ci -> extractSendIndex(ci.getSubcontrol())))
                .forEach(ci -> result.add(createSendMix(ci)));

        // 4. FADER
        all.stream()
                .filter(ci -> "kInputFader".equals(ci.getGroup()))
                .filter(ci -> "kFader".equals(ci.getSubcontrol()))
                .findFirst()
                .ifPresent(ci -> result.add(createFader(ci)));

        return result;
    }

    private ViewControl createChannelOn(ControlInstance ci) {
        return new ViewControl(
                "CHANNEL_ON",
                "kInputControl",
                "On",
                ControlType.TOGGLE,
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
                "kInputPan",
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