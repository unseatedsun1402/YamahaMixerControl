package MidiControl.Controls.Widgets;

import MidiControl.ContextModel.ControlType;
import MidiControl.ContextModel.ViewControl;
import MidiControl.Controls.ControlInstance;

public class EQ {
    private static ViewControl createEqControl(
            ControlInstance ci,
            String logicId,
            String displayName,
            String action,
            String viewType,
            String viewSuffix
    ) {
        return new ViewControl(
                logicId,
                "input.eq",
                displayName,
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
                action,
                null,
                ci.getInstanceIndex()
        );
    }

    public static ViewControl createEq1Gain(ControlInstance ci,String viewType,String viewSuffix)
    {
        return createEqControl(
                ci,
                "EQ1G",
                "EQ 1 Gain",
                "INPUT_EQ1_GAIN",
                viewType,
                viewSuffix
        );
    }

    public static ViewControl createEq2Gain(ControlInstance ci,String viewType,String viewSuffix)
    {
        return createEqControl(
                ci,
                "EQ2G",
                "EQ 2 Gain",
                "INPUT_EQ2_GAIN",
                viewType,
                viewSuffix
        );
    }

    public static ViewControl createEq3Gain(ControlInstance ci,String viewType,String viewSuffix)
    {
        return createEqControl(
                ci,
                "EQ3G",
                "EQ 3 Gain",
                "INPUT_EQ3_GAIN",
                viewType,
                viewSuffix
        );
    }

    public static ViewControl createEq4Gain(ControlInstance ci,String viewType,String viewSuffix)
    {
        return createEqControl(
                ci,
                "EQ4G",
                "EQ 4 Gain",
                "INPUT_EQ4_GAIN",
                viewType,
                viewSuffix
        );
    }

    public static ViewControl createEq1Frequency(ControlInstance ci,String viewType,String viewSuffix)
    {
        return createEqControl(
                ci,
                "EQ1F",
                "EQ 1 Frequency",
                "INPUT_EQ1_FREQUENCY",
                viewType,
                viewSuffix
        );
    }

    public static ViewControl createEq2Frequency(ControlInstance ci,String viewType,String viewSuffix)
    {
        return createEqControl(
                ci,
                "EQ2F",
                "EQ 2 Frequency",
                "INPUT_EQ2_FREQUENCY",
                viewType,
                viewSuffix
        );
    }

    public static ViewControl createEq3Frequency(ControlInstance ci,String viewType,String viewSuffix)
    {
        return createEqControl(
                ci,
                "EQ3F",
                "EQ 3 Frequency",
                "INPUT_EQ3_FREQUENCY",
                viewType,
                viewSuffix
        );
    }

    public static ViewControl createEq4Frequency(ControlInstance ci,String viewType,String viewSuffix)
    {
        return createEqControl(
                ci,
                "EQ4F",
                "EQ 4 Frequency",
                "INPUT_EQ4_FREQUENCY",
                viewType,
                viewSuffix
        );
    }

    public static ViewControl createEq1Q(ControlInstance ci,String viewType,String viewSuffix)
    {
        return createEqControl(
                ci,
                "EQ1Q",
                "EQ 1 Q",
                "INPUT_EQ1_Q",
                viewType,
                viewSuffix
        );
    }

    public static ViewControl createEq2Q(ControlInstance ci,String viewType,String viewSuffix)
    {
        return createEqControl(
                ci,
                "EQ2Q",
                "EQ 2 Q",
                "INPUT_EQ2_Q",
                viewType,
                viewSuffix
        );
    }

    public static ViewControl createEq3Q(ControlInstance ci,String viewType,String viewSuffix)
    {
        return createEqControl(
                ci,
                "EQ3Q",
                "EQ 3 Q",
                "INPUT_EQ3_Q",
                viewType,
                viewSuffix
        );
    }

    public static ViewControl createEq4Q(ControlInstance ci,String viewType,String viewSuffix)
    {
        return createEqControl(
                ci,
                "EQ4Q",
                "EQ 4 Q",
                "INPUT_EQ4_Q",
                viewType,
                viewSuffix
        );
    }
}
