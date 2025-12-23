package MidiControl.UserInterface.Frontend;

import MidiControl.Routing.WebSocketEndpoint;
import MidiControl.Server.SubscriptionManager;
import jakarta.websocket.Session;

public class WebSocketGuiBroadcaster implements GuiBroadcaster {

    private final SubscriptionManager subscriptions;

    public WebSocketGuiBroadcaster(SubscriptionManager subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Override
    public void broadcast(String json, String contextId) {
        for (Session s : subscriptions.getSubscribers(contextId)) {
            WebSocketEndpoint.send(s, json);
        }
    }
}
