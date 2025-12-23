package MidiControl.TestUtilities;

import MidiControl.Controls.*;

public class TestListener implements ControlListener {
    public ControlInstance lastInstance;
    public int lastValue;

    @Override
    public void onControlChanged(ControlInstance instance, int newValue) {
        this.lastInstance = instance;
        this.lastValue = newValue;
    }
}