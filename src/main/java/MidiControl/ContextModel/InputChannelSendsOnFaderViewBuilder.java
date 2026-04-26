package MidiControl.ContextModel;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;


public class InputChannelSendsOnFaderViewBuilder implements ViewBuilder {

    private static final Logger logger =
            Logger.getLogger(InputChannelSendsOnFaderViewBuilder.class.getName());

    /**
     * Universal Yamaha-safe send-level matcher.
     *
     * Matches:
     *   kMix1Level
     *   kMix01Level
     *   kAUX2Level
     *   kAUX12Level
     *
     * Groups:
     *   (1) Mix | AUX
     *   (2) numeric bus index (zero-padded allowed)
     */
    private static final Pattern SEND_PATTERN =
            Pattern.compile("^k(Mix|AUX)(0*[0-9]+)Level$", Pattern.CASE_INSENSITIVE);

    private String targetBusId = "MIX1";   // default
    private boolean hasMixBuses = false;
    private boolean hasAuxBuses = false;

    public InputChannelSendsOnFaderViewBuilder() {}

    @Override
    public List<ViewControl> build(Context context,
                                   CanonicalRegistry registry,
                                   String suffix) {

        List<ControlInstance> all = registry.getAllInstancesForContext(context.getId());

        hasMixBuses = all.stream().anyMatch(ci -> ci.getSubcontrol().startsWith("kMix"));
        hasAuxBuses = all.stream().anyMatch(ci -> ci.getSubcontrol().startsWith("kAUX"));

        if (suffix != null && !suffix.isBlank()) {
            targetBusId = suffix.toUpperCase(); // "MIX1", "AUX2"
        }

        // --------------------------------------------------------------------
        // Automatic fallback:
        // If user asks for MIXn but console has only AUXn, convert MIX→AUX
        // --------------------------------------------------------------------
        if (targetBusId.startsWith("MIX") && !hasMixBuses && hasAuxBuses) {
            targetBusId = targetBusId.replace("MIX", "AUX");
        }

        if (targetBusId.startsWith("AUX") && !hasAuxBuses && hasMixBuses) {
            targetBusId = targetBusId.replace("AUX", "MIX");
        }

        // --------------------------------------------------------------------
        // Build final UI control list
        // --------------------------------------------------------------------
        List<ViewControl> result = new ArrayList<>();

        // CHANNEL ON (optional)
        all.stream()
                .filter(ci -> "kInputOn".equals(ci.getGroup()))
                .filter(ci -> "kChannelOn".equals(ci.getSubcontrol()))
                .findFirst()
                .ifPresent(ci -> result.add(createToggle(ci)));

        // PAN (optional)
        all.stream()
                .filter(ci -> "kInputPan".equals(ci.getGroup()))
                .filter(ci -> "kChannelPan".equals(ci.getSubcontrol()))
                .findFirst()
                .ifPresent(ci -> result.add(createPan(ci)));

        // SEND that matches the target MIX/AUX bus
        all.stream()
                .filter(ci ->
                        "kInputToMix".equals(ci.getGroup()) ||
                        "kInputAUX".equals(ci.getGroup()))
                .peek(ci -> logger.fine("SEND CANDIDATE: " + ci.getSubcontrol()))
                .filter(ci -> isSendLevel(ci.getSubcontrol()))
                .filter(ci -> sendMatchesTargetBus(ci.getSubcontrol()))
                .findFirst()
                .ifPresentOrElse(
                        ci -> result.add(createSendAsFader(ci)),
                        () -> logger.warning("SOF: No matching send found for " + targetBusId)
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

    private ViewControl createToggle(ControlInstance ci) {
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

    private ViewControl createSendAsFader(ControlInstance ci) {
        String logicId = "SEND_" + targetBusId;

        return new ViewControl(
                logicId,
                "kInputToMix-SOF",
                targetBusId,
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
}
