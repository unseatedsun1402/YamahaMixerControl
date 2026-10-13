package MidiControl.ContextModel.ViewBuilders;

import java.util.ArrayList;
import java.util.List;

import MidiControl.ContextModel.Context;
import MidiControl.ContextModel.ViewBuilder;
import MidiControl.ContextModel.ViewControl;
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
                        result.add(MidiControl.Controls.Widgets.ChannelOn.createChannelOn(ci, viewType, viewSuffix, channelIndex))
                );

        all.stream()
                .filter(ci -> "kInputPan".equals(ci.getGroup()))
                .filter(ci -> "kChannelPan".equals(ci.getSubcontrol()))
                .findFirst()
                .ifPresent(ci ->
                        result.add(MidiControl.Controls.Widgets.Pan.createPan(ci, viewType, viewSuffix, channelIndex))
                );

        return result;
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
