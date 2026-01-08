package MidiControl.Controls;

public interface ControlListener {
    void onControlChanged(ControlInstance instance, int newValue);
}
