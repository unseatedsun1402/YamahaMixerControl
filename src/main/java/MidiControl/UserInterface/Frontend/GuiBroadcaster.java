package MidiControl.UserInterface.Frontend;

import java.util.Collection;

public interface GuiBroadcaster {
  void broadcast(String json, String contextId);

  default void broadcast(String json, Collection<String> contextIds) {
    contextIds.forEach(contextId -> broadcast(json, contextId));
  }
}
