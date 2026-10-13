package MidiControl.Controls.Widgets;

import MidiControl.ContextModel.ControlType;
import MidiControl.ContextModel.ViewControl;
import MidiControl.Controls.ControlInstance;

public class Pan {
    public static ViewControl createPan(ControlInstance ci,
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
}
