package MidiControl.ContextModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.SubControl;
public class ContextFactory {

    public static Context buildChannelStrip(CanonicalRegistry registry, int channelIndex) {

        ControlSchema schema = new ControlSchema(registry);
        List<ContextFilter> filters = new ArrayList<>();

        addDynamicFilter(schema, filters, "kFader");

        addDynamicFilter(schema, filters, "kPan");

        addDynamicIndexedFilters(schema, filters, "kEQ");

        addDynamicIndexedFilters(schema, filters, "kMix");

        addDynamicIndexedFilters(schema, filters, "kAUX");

        addDynamicFilter(schema, filters, "kNameShort");
        addDynamicFilter(schema, filters, "kNameLong");
        addDynamicFilter(schema, filters, "kChannelNameShort");
        addDynamicFilter(schema, filters, "kChannelNameLong");

        return new Context(
                "channel." + channelIndex,
                "Channel " + channelIndex,
                ContextType.CHANNEL,
                List.of("admin", "engineer"),
                filters
        );
    }

    public static Context buildMixStrip(CanonicalRegistry registry, int mixIndex) {
        ControlSchema schema = new ControlSchema(registry);
        List<ContextFilter> filters = new ArrayList<>();

        addDynamicFilter(schema, filters, "kMixFader");
        addDynamicFilter(schema, filters, "kMixNameShort");
        addDynamicFilter(schema, filters, "kMixNameLong");

        // addDynamicIndexedFilters(schema, filters, "kMixSend");
        

        return new Context(
            "mix." + mixIndex,
            "Mix " + mixIndex,
            ContextType.MIX,
            List.of("admin", "engineer"),
            filters
        );
    }

    public static Context buildMatrixStrip(CanonicalRegistry registry, int matrixIndex) {

        ControlSchema schema = new ControlSchema(registry);
        List<ContextFilter> filters = new ArrayList<>();

        // 1. Matrix Fader
        addDynamicFilter(schema, filters, "kMatrixFader");

        // 2. Matrix Names
        addDynamicFilter(schema, filters, "kMatrixNameShort");
        addDynamicFilter(schema, filters, "kMatrixNameLong");

        // 3. Matrix Inputs (Mix → Matrix sends)
        addDynamicIndexedFilters(schema, filters, "kMatrixSend");

        // 4. Matrix EQ
        addDynamicIndexedFilters(schema, filters, "kMatrixEQ");

        return new Context(
            "matrix." + matrixIndex,
            "Matrix " + matrixIndex,
            ContextType.MATRIX,
            List.of("admin", "engineer"),
            filters
        );
    }

    public static Context buildDcaStrip(CanonicalRegistry registry, int dcaIndex) {

        ControlSchema schema = new ControlSchema(registry);
        List<ContextFilter> filters = new ArrayList<>();

        // 1. DCA Fader
        addDynamicFilter(schema, filters, "kDCAFader");

        // 2. DCA Names
        addDynamicFilter(schema, filters, "kDCANameShort");
        addDynamicFilter(schema, filters, "kDCANameLong");

        return new Context(
            "dca." + dcaIndex,
            "DCA " + dcaIndex,
            ContextType.DCA,
            List.of("admin", "engineer"),
            filters
        );
    }

    public static Context buildStereoInput(CanonicalRegistry registry, int stereoIndex) {

        ControlSchema schema = new ControlSchema(registry);
        List<ContextFilter> filters = new ArrayList<>();

        // 1. Stereo Fader
        addDynamicFilter(schema, filters, "kStereoFader");

        // 2. Stereo Pan / Balance
        addDynamicFilter(schema, filters, "kStereoPan");

        // 3. Stereo Names
        addDynamicFilter(schema, filters, "kStereoNameShort");
        addDynamicFilter(schema, filters, "kStereoNameLong");

        // 4. Stereo EQ
        addDynamicIndexedFilters(schema, filters, "kStereoEQ");

        // 5. Stereo Sends
        addDynamicIndexedFilters(schema, filters, "kStereoSend");

        return new Context(
            "stereo." + stereoIndex,
            "Stereo " + stereoIndex,
            ContextType.STEREO_INPUT,
            List.of("admin", "engineer"),
            filters
        );
    }


    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private static void addDynamicFilter(
        ControlSchema schema,
        List<ContextFilter> filters,
        String prefix
    ) {
        Set<String> groups = schema.getGroupsForPrefix(prefix);
        if (!groups.isEmpty()) {
            filters.add(new ContextFilter(groups.iterator().next(), prefix, null));
        }
    }

    private static void addDynamicIndexedFilters(
            ControlSchema schema,
            List<ContextFilter> filters,
            String prefix
    ) {
        // Find all groups that contain subcontrols with this prefix
        Set<String> groups = schema.getGroupsForPrefix(prefix);
        if (groups.isEmpty())
            return;

        for (String group : groups) {

            // Detect how many indexed subcontrols exist for this group+prefix
            int maxIndex = schema.detectIndexedRange(group, prefix);

            for (int i = 1; i <= maxIndex; i++) {
                filters.add(new ContextFilter(
                        group,   // correct group name
                        prefix,  // subcontrol prefix
                        i        // index
                ));
            }
        }
    }

    public static Context fromId(String id, CanonicalRegistry registry) {
        if (id == null || !id.contains(".")) return null;

        String[] parts = id.split("\\.");
        if (parts.length != 2) return null;

        String prefix = parts[0];
        int index = Integer.parseInt(parts[1]);

        return switch (prefix) {
            case "channel" -> buildChannelStrip(registry, index);
            case "mix"     -> buildMixStrip(registry, index);
            case "matrix"  -> buildMatrixStrip(registry, index);
            case "dca"     -> buildDcaStrip(registry, index);
            case "stereo"  -> buildStereoInput(registry, index);
            default        -> null;
        };
    }

    private static Integer extractIndex(String name) {
        StringBuilder digits = new StringBuilder();

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            } else if (digits.length() > 0) {
                break;
            }
        }

        if (digits.length() == 0)
            return null;

        return Integer.parseInt(digits.toString());
    }
}