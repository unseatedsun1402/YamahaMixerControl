package MidiControl.unit.Telemetry;

import MidiControl.MidiDeviceManager.MidiSendEngine;

public class FakeSendEngine extends MidiSendEngine {

    private int remainingPercent;

    public FakeSendEngine(int remainingPercent) {
        super(null, 1, 1);
        this.remainingPercent = remainingPercent;
    }

    @Override
    public int getSysexQueueRemainingPercent() {
        return remainingPercent;
    }

    public void setRemainingPercent(int remainingPercent) {
        this.remainingPercent = remainingPercent;
    }
}