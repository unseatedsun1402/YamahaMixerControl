package MidiControl.Mocks;

import MidiControl.Server.SubscriptionManager;
import jakarta.websocket.Session;

public class MockSubscriptionManager extends SubscriptionManager {

    public String lastSubscribed;
    public String lastUnsubscribed;

    @Override
    public void subscribe(Session session, String contextId) {
        lastSubscribed = contextId;
    }

    @Override
    public void unsubscribe(Session session, String contextId) {
        lastUnsubscribed = contextId;
    }
}