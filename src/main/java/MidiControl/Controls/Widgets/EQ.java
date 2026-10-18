package MidiControl.Controls.Widgets;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import MidiControl.ContextModel.ControlType;
import MidiControl.ContextModel.ViewControl;
import MidiControl.Controls.ControlInstance;

public class EQ {
    private static final Pattern GAIN_RANGE_PATTERN =
            Pattern.compile("(-?\\d+(?:\\.\\d+)?)\\s*dB\\s*-\\s*\\+?(-?\\d+(?:\\.\\d+)?)\\s*dB", Pattern.CASE_INSENSITIVE);

    private static ViewControl createEqControl(
            ControlInstance ci,
            String logicId,
            String displayName,
            String action,
            String viewType,
            String viewSuffix
    ) {
        ViewControl control = new ViewControl(
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

        if (logicId.endsWith("G")) {
            control.unit = "dB";
            String comment = ci.getSysex().getComment();
            if (comment != null) {
                Matcher gainRange = GAIN_RANGE_PATTERN.matcher(comment);
                if (gainRange.find()) {
                    control.unitMin = Double.parseDouble(gainRange.group(1));
                    control.unitMax = Double.parseDouble(gainRange.group(2));
                }
            }
        }

        return control;
    }

    public static ViewControl createEqBandControl(
            ControlInstance ci,
            int band,
            char parameter,
            String viewType,
            String viewSuffix
    ) {
        return switch (parameter) {
            case 'G' -> switch (band) {
                case 1 -> createEq1Gain(ci, viewType, viewSuffix);
                case 2 -> createEq2Gain(ci, viewType, viewSuffix);
                case 3 -> createEq3Gain(ci, viewType, viewSuffix);
                case 4 -> createEq4Gain(ci, viewType, viewSuffix);
                default -> throw new IllegalArgumentException("Unsupported EQ band: " + band);
            };
            case 'F' -> switch (band) {
                case 1 -> createEq1Frequency(ci, viewType, viewSuffix);
                case 2 -> createEq2Frequency(ci, viewType, viewSuffix);
                case 3 -> createEq3Frequency(ci, viewType, viewSuffix);
                case 4 -> createEq4Frequency(ci, viewType, viewSuffix);
                default -> throw new IllegalArgumentException("Unsupported EQ band: " + band);
            };
            case 'Q' -> switch (band) {
                case 1 -> createEq1Q(ci, viewType, viewSuffix);
                case 2 -> createEq2Q(ci, viewType, viewSuffix);
                case 3 -> createEq3Q(ci, viewType, viewSuffix);
                case 4 -> createEq4Q(ci, viewType, viewSuffix);
                default -> throw new IllegalArgumentException("Unsupported EQ band: " + band);
            };
            default -> throw new IllegalArgumentException("Unsupported EQ parameter: " + parameter);
        };
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
