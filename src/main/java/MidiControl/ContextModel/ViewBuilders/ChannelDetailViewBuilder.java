package MidiControl.ContextModel.ViewBuilders;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import MidiControl.ContextModel.Context;
import MidiControl.ContextModel.ContextFilter;
import MidiControl.ContextModel.ControlType;
import MidiControl.ContextModel.ViewBuilder;
import MidiControl.ContextModel.ViewControl;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;

/**
 * Channel Detail ViewBuilder
 *
 * Produces a comprehensive single-channel editing view by combining the control
 * sets from {@link InputChannelStripViewBuilder} (all sends + fader),
 * {@link InputChannelSendsOnFaderViewBuilder} (on, pan, EQ, dynamics), and
 * {@link MixAuxBusViewBuilder} (bus fader, pan, comp, EQ) — plus additional
 * per-channel controls (mute, pair, attenuation) that the existing builders
 * omit.
 *
 * <p>For input channels ({@code channel.N}):
 * <ul>
 *   <li>CHANNEL_ON toggle</li>
 *   <li>MUTE toggle</li>
 *   <li>PAIR toggle</li>
 *   <li>PAN horizontal slider</li>
 *   <li>ATTENUATION knob (head amp)</li>
 *   <li>EQ — 4 bands x G/F/Q</li>
 *   <li>GATE / DYNAMICS1 toggle</li>
 *   <li>COMPRESSOR / DYNAMICS2 — on toggle, ratio, threshold</li>
 *   <li>SEND_MIX{n} — all mix/aux send knobs, sorted by bus index</li>
 *   <li>FADER</li>
 * </ul>
 *
 * <p>For output buses ({@code mix.N}, {@code aux.N}, {@code stereo.0}):
 * <ul>
 *   <li>BUS_FADER</li>
 *   <li>PAN</li>
 *   <li>COMPRESSOR — on toggle, ratio, threshold</li>
 *   <li>EQ — 4 bands x G/F/Q</li>
 * </ul>
 *
 * <p>Registered under the key {@code "channel-detail-view"} in
 * {@link MidiControl.Server.MidiServer}. The frontend requests this view via
 * {@code ws.requestUiModel(contextId, "channel-detail-view")}.
 */
public class ChannelDetailViewBuilder implements ViewBuilder {

    private static final Logger logger =
            Logger.getLogger(ChannelDetailViewBuilder.class.getName());

    private static final Pattern SEND_PATTERN =
            Pattern.compile("^k(Mix|AUX)(\\d+)Level$", Pattern.CASE_INSENSITIVE);

    private static final String VIEW_TYPE = "channel-detail-view";

    public ChannelDetailViewBuilder() {}

    @Override
    public List<ViewControl> build(Context context,
                                    CanonicalRegistry registry,
                                    String suffix) {

        String contextId = context.getId();
        String family = contextId.contains(".")
                ? contextId.split("\\.")[0]
                : "";

        return switch (family) {
            case "channel" -> buildInputChannelDetail(context, registry, suffix);
            case "mix", "aux", "stereo", "bus" -> buildBusDetail(context, registry, suffix);
            default -> {
                logger.warning("ChannelDetailViewBuilder: unsupported context: " + contextId);
                yield new ArrayList<>();
            }
        };
    }

    // =================== INPUT CHANNEL ===================

    private List<ViewControl> buildInputChannelDetail(
            Context context,
            CanonicalRegistry registry,
            String suffix) {

        List<ViewControl> result = new ArrayList<>();

        int channelIndex = registry.extractContextIndex(context.getId());
        if (channelIndex < 0) return result;

        String viewSuffix = suffix != null ? suffix : context.getId();

        // Fetch all instances once and reuse for group-based lookups
        List<ControlInstance> all =
                registry.getAllInstancesForContext(context.getId());

        // --- Channel On ---
        ControlInstance channelOn = registry.find(channelIndex, "kInputOn", "kChannelOn");
        if (channelOn == null) {
            channelOn = registry.find(channelIndex, "kInputChannelOn", "kChannelOn");
        }
        if (channelOn != null) {
            result.add(MidiControl.Controls.Widgets.ChannelOn.createChannelOn(
                    channelOn, VIEW_TYPE, viewSuffix, channelIndex));
        }

        // --- Mute ---
        ControlInstance mute = findFirstInGroup(all, "kInputMute");
        if (mute != null) {
            result.add(MidiControl.Controls.Widgets.Generic.createGenericControl(
                    mute, "MUTE", "channel.mute", "Mute",
                    ControlType.TOGGLE, VIEW_TYPE, viewSuffix,
                    "INPUT_MUTE", null));
        }

        // --- Pair ---
        ControlInstance pair = findFirstInGroup(all, "kInputPair");
        if (pair != null) {
            result.add(MidiControl.Controls.Widgets.Generic.createGenericControl(
                    pair, "PAIR", "channel.pair", "Pair",
                    ControlType.TOGGLE, VIEW_TYPE, viewSuffix,
                    "INPUT_PAIR", null));
        }

        // --- Pan ---
        ControlInstance pan = registry.find(channelIndex, "kInputPan", "kChannelPan");
        if (pan == null) {
            pan = registry.find(channelIndex, "kInputChannelPan", "kChannelPan");
        }
        if (pan != null) {
            result.add(MidiControl.Controls.Widgets.Pan.createPan(
                    pan, VIEW_TYPE, viewSuffix, channelIndex));
        }

        // --- Attenuation / Head Amp ---
        ControlInstance ha = findFirstInGroup(all, "kInputHA");
        if (ha == null) {
            ha = findFirstInGroup(all, "kInputAttenuation");
        }
        if (ha != null) {
            result.add(MidiControl.Controls.Widgets.Generic.createGenericControl(
                    ha, "ATTENUATION", "channel.attenuation", "Attenuation",
                    ControlType.KNOB, VIEW_TYPE, viewSuffix,
                    "INPUT_ATTENUATION", null));
        }

        // --- EQ (4 bands x G/F/Q) ---
        addInputEqControls(result, registry, channelIndex, viewSuffix);

        // --- Gate / Dynamics 1 ---
        ControlInstance gate = registry.find(channelIndex, "kInputDynamics1", "kDynaOn");
        if (gate == null) {
            gate = registry.find(channelIndex, "kInputGate", "kGateOn");
        }
        if (gate != null) {
            result.add(MidiControl.Controls.Widgets.Generic.createGenericControl(
                    gate, "GATE_ON", "input.dynamics", "Gate",
                    ControlType.TOGGLE, VIEW_TYPE, viewSuffix,
                    "DYNAMICS1_ON", null));
        }

        ControlInstance gateThreshold =
                registry.find(channelIndex, "kInputDynamics1", "kDynaThreshold");
        if (gateThreshold == null) {
            gateThreshold = registry.find(channelIndex, "kInputGate", "kGateThreshold");
        }
        if (gateThreshold != null) {
            result.add(MidiControl.Controls.Widgets.Generic.createGenericControl(
                    gateThreshold, "GATE_THRESHOLD", "input.dynamics", "Gate Threshold",
                    ControlType.SLIDER_HORIZONTAL, VIEW_TYPE, viewSuffix,
                    "DYNAMICS1_THRESHOLD", null));
        }

        // --- Compressor / Dynamics 2 ---
        ControlInstance compOn = registry.find(channelIndex, "kInputDynamics2", "kDynaOn");
        if (compOn == null) {
            compOn = registry.find(channelIndex, "kInputComp", "kCompOn");
        }
        if (compOn != null) {
            result.add(MidiControl.Controls.Widgets.Generic.createGenericControl(
                    compOn, "COMP_ON", "input.dynamics", "Comp",
                    ControlType.TOGGLE, VIEW_TYPE, viewSuffix,
                    "DYNAMICS2_ON", null));
        }

        ControlInstance compRatio = registry.find(channelIndex, "kInputDynamics2", "kDynaRatio");
        if (compRatio == null) {
            compRatio = registry.find(channelIndex, "kInputComp", "kCompRatio");
        }
        if (compRatio != null) {
            result.add(MidiControl.Controls.Widgets.Generic.createGenericControl(
                    compRatio, "COMP_RATIO", "input.dynamics", "Ratio",
                    ControlType.SLIDER_HORIZONTAL, VIEW_TYPE, viewSuffix,
                    "DYNAMICS2_RATIO", null));
        }

        ControlInstance compThresh = registry.find(channelIndex, "kInputDynamics2", "kDynaThreshold");
        if (compThresh == null) {
            compThresh = registry.find(channelIndex, "kInputComp", "kCompThreshold");
        }
        if (compThresh != null) {
            result.add(MidiControl.Controls.Widgets.Generic.createGenericControl(
                    compThresh, "COMP_THRESHOLD", "input.dynamics", "Threshold",
                    ControlType.SLIDER_HORIZONTAL, VIEW_TYPE, viewSuffix,
                    "DYNAMICS2_THRESHOLD", null));
        }

        // --- All Send Mix knobs (sorted by bus index) ---
        all.stream()
                .filter(ci -> "kInputToMix".equals(ci.getGroup())
                        || "kInputAUX".equals(ci.getGroup()))
                .filter(ci -> isSendLevel(ci.getSubcontrol()))
                .sorted(Comparator.comparingInt(ci -> extractSendIndex(ci.getSubcontrol())))
                .forEach(ci -> result.add(
                        MidiControl.Controls.Widgets.SendMix.createSendMix(
                                ci, VIEW_TYPE, viewSuffix, channelIndex)));

        // --- Fader ---
        all.stream()
                .filter(ci -> "kInputFader".equals(ci.getGroup()))
                .filter(ci -> "kFader".equals(ci.getSubcontrol()))
                .findFirst()
                .ifPresent(ci -> result.add(
                        MidiControl.Controls.Widgets.Fader.createFader(
                                ci, VIEW_TYPE, viewSuffix, channelIndex)));

        return result;
    }

    // =================== OUTPUT BUS ===================

    private List<ViewControl> buildBusDetail(
            Context context,
            CanonicalRegistry registry,
            String suffix) {

        List<ViewControl> result = new ArrayList<>();

        String contextId = context.getId();
        String family = contextId.split("\\.")[0];

        String prefix = switch (family) {
            case "mix"    -> "kMix";
            case "aux"    -> "kAUX";
            case "stereo" -> "kStereo";
            case "bus"    -> "kBus";
            default       -> null;
        };

        if (prefix == null) return result;

        String viewSuffix = suffix != null ? suffix : contextId;

        // Gather all instances via context filters (same approach as MixAuxBusViewBuilder)
        List<ControlInstance> all = new ArrayList<>();
        for (ContextFilter filter : context.getFilters()) {
            all.addAll(registry.getInstancesForFilter(filter));
        }

        // --- Bus Fader ---
        ControlInstance fader = findInstance(all, prefix + "Fader", "kFader");
        if (contextId.startsWith("stereo.")) {
            fader = findInstance(all, "kStereoFader", "kFader");
        }
        if (fader != null) {
            result.add(MidiControl.Controls.Widgets.Fader.createBusFader(
                    fader, VIEW_TYPE, viewSuffix));
        }

        // --- Pan / Balance ---
        ControlInstance pan = findFirst(all,
                prefix + "Pan", "kPan",
                prefix + "Pan", "kBalance");
        if (pan != null) {
            result.add(createControl(pan, "PAN", "bus.pan", "Pan",
                    ControlType.SLIDER_HORIZONTAL, viewSuffix, "BUS_PAN", null));
        }

        // --- Compressor / Dynamics 2 ---
        ControlInstance compOn = findFirst(all,
                prefix + "Comp", "kCompOn",
                prefix + "Dyn", "kDynaOn");
        if (compOn != null) {
            result.add(createControl(compOn, "COMP_ON", "input.dynamics", "Comp",
                    ControlType.TOGGLE, viewSuffix, "DYNAMICS2_ON", null));
        }

        ControlInstance compRatio = findFirst(all,
                prefix + "Comp", "kCompRatio",
                prefix + "Dyn", "kDynaRatio");
        if (compRatio != null) {
            result.add(createControl(compRatio, "COMP_RATIO", "input.dynamics", "Ratio",
                    ControlType.SLIDER_HORIZONTAL, viewSuffix, "DYNAMICS2_RATIO", null));
        }

        ControlInstance compThresh = findFirst(all,
                prefix + "Comp", "kCompThreshold",
                prefix + "Dyn", "kDynaThreshold");
        if (compThresh != null) {
            result.add(createControl(compThresh, "COMP_THRESHOLD", "input.dynamics", "Threshold",
                    ControlType.SLIDER_HORIZONTAL, viewSuffix, "DYNAMICS2_THRESHOLD", null));
        }

        // --- EQ (4 bands x G/F/Q) ---
        addBusEqControls(result, all, prefix, viewSuffix);

        return result;
    }

    // =================== EQ HELPERS ===================

    private void addInputEqControls(
            List<ViewControl> result,
            CanonicalRegistry registry,
            int channelIndex,
            String viewSuffix) {

        String[] legacyBandNames = {"kEQLow", "kEQLowMid", "kEQHiMid", "kEQHi"};

        for (int band = 0; band < legacyBandNames.length; band++) {
            int bandNumber = band + 1;
            String canonicalPrefix = "kEQ" + bandNumber;
            String legacyPrefix = legacyBandNames[band];

            for (char parameter : new char[]{'G', 'F', 'Q'}) {
                ControlInstance control = registry.find(
                        channelIndex, "kInputEQ",
                        canonicalPrefix + parameter);

                if (control == null) {
                    control = registry.find(
                            channelIndex, "kInputEQ",
                            legacyPrefix + parameter);
                }

                if (control != null) {
                    result.add(MidiControl.Controls.Widgets.EQ.createEqBandControl(
                            control, bandNumber, parameter,
                            VIEW_TYPE, viewSuffix));
                }
            }
        }
    }

    private void addBusEqControls(
            List<ViewControl> result,
            List<ControlInstance> controls,
            String prefix,
            String viewSuffix) {

        String[] legacyBandNames = {"kEQLow", "kEQLowMid", "kEQHiMid", "kEQHi"};

        for (int band = 0; band < legacyBandNames.length; band++) {
            int bandNumber = band + 1;
            String canonicalPrefix = "kEQ" + bandNumber;
            String legacyPrefix = legacyBandNames[band];

            for (char parameter : new char[]{'G', 'F', 'Q'}) {
                ControlInstance control = findFirst(
                        controls,
                        prefix + "EQ", canonicalPrefix + parameter,
                        "kAUXEQ", legacyPrefix + parameter);

                if (control != null) {
                    result.add(MidiControl.Controls.Widgets.EQ.createEqBandControl(
                            control, bandNumber, parameter,
                            VIEW_TYPE, viewSuffix));
                }
            }
        }
    }

    // =================== UTILITY ===================

    private ControlInstance findFirstInGroup(List<ControlInstance> all, String group) {
        return all.stream()
                .filter(ci -> group.equals(ci.getGroup()))
                .findFirst()
                .orElse(null);
    }

    private ViewControl createControl(
            ControlInstance ci,
            String logicId,
            String uiGroup,
            String label,
            ControlType type,
            String viewSuffix,
            String role,
            Integer sendIndex) {

        return new ViewControl(
                logicId, uiGroup, label, type, 0,
                ci.getMin(), ci.getMax(),
                ci.getValue(), ci.getSysex().getDefault_value(),
                ci.getGroup(), ci.getSubcontrol(), ci.getInstanceIndex(),
                VIEW_TYPE, viewSuffix, role, sendIndex,
                ci.getInstanceIndex());
    }

    private ControlInstance findInstance(
            List<ControlInstance> instances,
            String group,
            String subcontrol) {

        return instances.stream()
                .filter(ci -> group.equals(ci.getGroup())
                        && subcontrol.equals(ci.getSubcontrol()))
                .findFirst()
                .orElse(null);
    }

    private ControlInstance findFirst(
            List<ControlInstance> instances,
            String group1, String sub1,
            String group2, String sub2) {

        ControlInstance result = findInstance(instances, group1, sub1);
        if (result != null) return result;
        return findInstance(instances, group2, sub2);
    }

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
