package MidiControl.Controls.Widgets;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import MidiControl.ContextModel.ControlType;
import MidiControl.ContextModel.ViewControl;
import MidiControl.Controls.ControlInstance;

public class SendMix {
    private static final Pattern SEND_PATTERN =
        Pattern.compile("^k(Mix|AUX)(0*[0-9]+)Level$", Pattern.CASE_INSENSITIVE);

    private static int extractSendIndex(String subcontrol) {
        Matcher m = SEND_PATTERN.matcher(subcontrol);
        if (!m.matches()) return -1;
        return Integer.parseInt(m.group(2));
    }

    public static ViewControl createSendMix(ControlInstance ci,String viewType,String viewSuffix,int channelIndex)
    {
        int mixIndex = extractSendIndex(ci.getSubcontrol());

        String logicId = "SEND_MIX" + mixIndex;
        String label = "Mix " + mixIndex;

        return new ViewControl(
                logicId,
                "input.send",
                label,
                ControlType.KNOB,
                mixIndex - 1,
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
                mixIndex,
                channelIndex
        );
    }
}
