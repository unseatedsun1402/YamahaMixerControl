package MidiControl.Routing;

public interface OutputRequestSender {
    void applyRequest(String canonicalId);
    void send(byte[] message);

    default void sendPriority(byte[] message) {
        send(message);
    }
}
