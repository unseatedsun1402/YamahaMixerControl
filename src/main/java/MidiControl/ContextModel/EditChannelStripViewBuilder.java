package MidiControl.ContextModel;

import java.util.ArrayList;
import java.util.List;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;

public class EditChannelStripViewBuilder implements ViewBuilder {

    public EditChannelStripViewBuilder() {}

    @Override
    public List<ViewControl> build(Context context, CanonicalRegistry registry, String suffix) {
        return buildCompact(context, registry, suffix);
    }

    public List<ViewControl> buildCompact(Context context, CanonicalRegistry registry, String suffix) {

        List<ViewControl> result = new ArrayList<>();

        int channelIndex = extractContextIndex(context.getId());
        if (channelIndex < 0) {
            return result;
        }

        String viewType = "edit-channel-view";
        String viewSuffix = suffix != null ? suffix : context.getId();

        List<ControlInstance> all =
                registry.getAllInstancesForContext(context.getId());

        all.stream()
                .filter(ci -> "kInputOn".equals(ci.getGroup()))
                .filter(ci -> "kChannelOn".equals(ci.getSubcontrol()))
                .findFirst()
                .ifPresent(ci ->
                        result.add(createChannelOn(ci, viewType, viewSuffix, channelIndex))
                );

        all.stream()
                .filter(ci -> "kInputPan".equals(ci.getGroup()))
                .filter(ci -> "kChannelPan".equals(ci.getSubcontrol()))
                .findFirst()
                .ifPresent(ci ->
                        result.add(createPan(ci, viewType, viewSuffix, channelIndex))
                );

        return result;
    }

    private ViewControl createChannelOn(ControlInstance ci,
                                        String viewType,
                                        String viewSuffix,
                                        int channelIndex) {

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
                channelIndex
        );
    }

    private ViewControl createPan(ControlInstance ci,
                                  String viewType,
                                  String viewSuffix,
                                  int channelIndex) {

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
                channelIndex
        );
    }

    private int extractContextIndex(String contextId) {
        int dot = contextId.lastIndexOf('.');
        if (dot == -1) {
            return -1;
        }

        try {
            return Integer.parseInt(contextId.substring(dot + 1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}