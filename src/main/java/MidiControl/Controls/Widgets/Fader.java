package MidiControl.Controls.Widgets;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import MidiControl.ContextModel.ControlType;
import MidiControl.ContextModel.ViewControl;
import MidiControl.Controls.ControlInstance;

public class Fader {
    private static final Pattern SEND_PATTERN =
        Pattern.compile("^k(Mix|AUX)(0*[0-9]+)Level$", Pattern.CASE_INSENSITIVE);

    private static int extractSendIndex(String subcontrol) {
        Matcher m = SEND_PATTERN.matcher(subcontrol);
        if (!m.matches()) return -1;
        return Integer.parseInt(m.group(2));
    }

    public static ViewControl createSendAsFader(ControlInstance ci, String viewType, String viewSuffix, String targetBusId) {

        int sendIndex = extractSendIndex(ci.getSubcontrol());
        String logicId = "SEND_" + targetBusId;

        return new ViewControl(
                logicId,
                "input.send.sof",
                targetBusId,
                ControlType.FADER,
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
                "INPUT_SEND_LEVEL",
                sendIndex,
                ci.getInstanceIndex()
        );
    }



    public static ViewControl createFader(
                ControlInstance ci,
                String viewType,
                String viewSuffix,
                int channelIndex
        ) {
            return new ViewControl(
                    "FADER",
                    "input.fader",
                    "Fader",
                    ControlType.FADER,
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
                    "INPUT_FADER",
                    null,
                    channelIndex
            );
        }

    public static ViewControl createBusFader(ControlInstance ci,String viewType,String viewSuffix)
    {
        return new ViewControl(
                "FADER",
                "bus.fader",
                "Fader",
                ControlType.FADER,
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
                "BUS_FADER",
                null,
                ci.getInstanceIndex()
        );
    }
}
