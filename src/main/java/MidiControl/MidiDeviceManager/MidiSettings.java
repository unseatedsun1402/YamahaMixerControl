package MidiControl.MidiDeviceManager;

public class MidiSettings {
  public int inputDeviceIndex;
  public String inputDeviceName;
  public String inputDeviceType;

  public int outputDeviceIndex;
  public String outputDeviceName;
  public String outputDeviceType;

  public MidiSettings(
      int inputIndex,
      String inputName,
      String inputType,
      int outputIndex,
      String outputName,
      String outputType) {
    this.inputDeviceIndex = inputIndex;
    this.inputDeviceName = inputName;
    this.inputDeviceType = inputType;
    this.outputDeviceIndex = outputIndex;
    this.outputDeviceName = outputName;
    this.outputDeviceType = outputType;
  }
}
