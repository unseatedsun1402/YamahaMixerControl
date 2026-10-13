package MidiControl.Controls.Widgets;

import MidiControl.ContextModel.ControlType;
import MidiControl.ContextModel.ViewControl;
import MidiControl.Controls.ControlInstance;

public class Generic {
        public static ViewControl createGenericControl(
            ControlInstance ci,
            String logicalId,
            String uiGroup,
            String label,
            ControlType type,
            String viewType,
            String viewSuffix,
            String role,
            Integer sendIndex
        ){
        return new ViewControl(
                logicalId,
                uiGroup,
                label,
                type,
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
                role,
                sendIndex,
                ci.getInstanceIndex()
        );
    }
}
