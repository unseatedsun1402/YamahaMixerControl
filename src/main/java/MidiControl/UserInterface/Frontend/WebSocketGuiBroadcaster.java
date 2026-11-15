package MidiControl.UserInterface.Frontend;

import MidiControl.Routing.WebSocketEndpoint;
import MidiControl.Server.SubscriptionManager;
import jakarta.websocket.Session;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class WebSocketGuiBroadcaster implements GuiBroadcaster {

  private SubscriptionManager subscriptions;

  public WebSocketGuiBroadcaster(SubscriptionManager subscriptions) {
    this.subscriptions = subscriptions;
  }

  @Override
  public void broadcast(String json, String contextId) {
    broadcast(json, java.util.List.of(contextId));
  }

  @Override
  public void broadcast(String json, Collection<String> contextIds) {
    Set<Session> sessions = new HashSet<>();
    for (String contextId : contextIds) {
      sessions.addAll(subscriptions.getSubscribers(contextId));
    }
    for (Session s : sessions) {
      try {
        WebSocketEndpoint.send(s, json);
      } catch (Exception e) {
        Logger.getLogger("WebSocketGuiBroadcaster")
            .severe("Exception thrown sending update to subscriber " + s.getId());
      }
    }
  }
}
