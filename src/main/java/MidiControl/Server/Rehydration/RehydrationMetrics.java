package MidiControl.Server.Rehydration;

public interface RehydrationMetrics {
    int getInflightTransactionCount();
    int getTimedOutTransactionCount();
}
