package MidiControl.Controls;

import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexParser;
import java.util.List;

public class Control {
  private final int controlId; // unique ID from SysEx mapping
  private final String controlGroup; // e.g., "Filter", "LFO".
  private final String subControl; // e.g., "Cutoff", "Rate"
  private int value; // current value
  private List<SysexMapping> mappings;

  public Control(int controlId, String controlGroup, String subControl) {
    this.controlId = controlId;
    this.controlGroup = controlGroup;
    this.subControl = subControl;
  }

  public void updateFromSysEx(byte[] data) {
    // parse value from SysEx payload
    this.value = SysexParser.extractValue(data);
  }

  public int getControlId() {
    return controlId;
  }

  public String getControlGroup() {
    return controlGroup;
  }

  public String getSubControl() {
    return subControl;
  }

  public int getValue() {
    return value;
  }

  public void setMappings(List<SysexMapping> mappings) {
    this.mappings = mappings;
  }
}
