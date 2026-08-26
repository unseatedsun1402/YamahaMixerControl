package MidiControl.MidiDeviceManager;

public record DeskDiscoveryResult(
    String model,
    int midiChannel
) {
    public String getModel() {return model;}
    public int getMidiChannel() {return midiChannel;}
}
