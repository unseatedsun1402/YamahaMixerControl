package MidiControl.UserInterface.Frontend;

import java.util.logging.Logger;

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
            try {
                WebSocketEndpoint.send(s, json);
            } catch (Exception e) {
                Logger.getLogger("WebSocketGuiBroadcaster").severe("Exception thrown sending update to subscriber "+s.getId());
            }
        }
}
}
