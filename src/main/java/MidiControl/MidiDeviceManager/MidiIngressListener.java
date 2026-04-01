package MidiControl.MidiDeviceManager;

@FunctionalInterface
public interface MidiIngressListener {
    void onBytesReceived(int byteCount);
}
