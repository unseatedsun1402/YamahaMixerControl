package MidiControl.MidiDeviceManager;

public interface Settings {
    public String getSettings();
    public boolean saveSettings();
    public void newSettings(int inputIndex, String inputName, int outputIndex, String outputName, String consoleName);
    public String toJson();
    public boolean evalSettings(String toCheckJson);

    public int getInputDeviceIndex();
    public int getOutputDeviceIndex();
    public String getInputDeviceName();
    public String getOutputDeviceName();
    public String getConsoleName();
    public String getConsoleMappingsFilePath();
}
