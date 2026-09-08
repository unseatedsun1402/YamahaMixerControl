package MidiControl.ContextModel;

import java.util.ArrayList;
import java.util.List;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;

public class PatchViewBuilder implements ViewBuilder {

    @Override
    public List<ViewControl> build(Context context,
                                   CanonicalRegistry registry,
                                   String suffix) {

        List<ViewControl> result = new ArrayList<>();

        int ch = extractContextIndex(context.getId());
        if (ch < 0) {
            return result;
        }

        String viewType   = "channel-view";
        String viewSuffix = suffix != null ? suffix : context.getId();

        // Get all controls for this channel
        List<ControlInstance> all =
            registry.getAllInstancesForContext(context.getId());

        // Find the patch destination control for this channel
        all.stream()
            .filter(this::isPatchDestination)
            .findFirst()
            .ifPresent(ci ->
                result.add(createPatchSelector(ci, viewType, viewSuffix, ch))
            );

        return result;
    }

    private boolean isPatchDestination(ControlInstance ci) {
        String g = ci.getGroup();
        String s = ci.getSubcontrol();

        // Yamaha input patch destinations
        return
            g.equals("kChannelInput") ||
            g.equals("kChannelInsertInput") ||
            g.equals("kEffectInput") ||
            s.contains("PatchIn") ||
            s.contains("PatchInInsert");
    }

    private ViewControl createPatchSelector(ControlInstance ci,
                                            String viewType,
                                            String viewSuffix,
                                            int channelIndex) {

        return new ViewControl(
            "PATCH_SELECTOR",
            "input.patch",
            "Input Patch",
            ControlType.SELECTOR,
            0,
            0,      // min source index
            150,    // max source index (01V96i typical)
            ci.getValue(),
            ci.getSysex().getDefault_value(),
            ci.getGroup(),
            ci.getSubcontrol(),
            ci.getInstanceIndex(),
            viewType,
            viewSuffix,
            "INPUT_PATCH",
            null,
            channelIndex
        );
    }

    private int extractContextIndex(String contextId) {
        int dot = contextId.lastIndexOf('.');
        if (dot == -1) return -1;

        try {
            return Integer.parseInt(contextId.substring(dot + 1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
