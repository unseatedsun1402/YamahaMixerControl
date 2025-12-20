package MidiControl.Controls;
import MidiControl.SysexUtils.SysexMapping;
import java.util.ArrayList;
import java.util.List;

public class ControlFactory {
    public static List<Control> fromSysexMappings(List<SysexMapping> mappings) {
        List<Control> controls = new ArrayList<>();
        for (SysexMapping mapping : mappings) {
            int controlId = mapping.getParameter_change_format().get(7) instanceof Number
                    ? ((Number) mapping.getParameter_change_format().get(7)).intValue()
                    : -1;
            if (controlId != -1) {
                controls.add(new Control(controlId,mapping.getControlGroup(), mapping.getSubControl()));
            }
        }
        return controls;
    }
}

