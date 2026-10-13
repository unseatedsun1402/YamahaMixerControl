package MidiControl.Controls.Widgets;

import MidiControl.ContextModel.ControlType;
import MidiControl.ContextModel.ViewControl;
import MidiControl.Controls.ControlInstance;

public class ChannelOn {
    public static ViewControl createChannelOn(ControlInstance ci,String viewType,String viewSuffix,int channelIndex) 
    {
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
}
