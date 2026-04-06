package MidiControl.ContextModel;

import java.util.ArrayList;
import java.util.List;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;

public class EditChannelStripViewBuilder implements ViewBuilder {

    public EditChannelStripViewBuilder() {}

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
        
}