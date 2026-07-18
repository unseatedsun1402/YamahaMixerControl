package MidiControl.Server.Rehydration;

public interface RehydrationListener {
    void onFinished();
    void onReset();
    void activateMeterRequests();
    void delayMeterRequests();
}
