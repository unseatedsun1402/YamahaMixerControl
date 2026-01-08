package MidiControl.TestUtilities;

import MidiControl.Routing.WebSocketEndpoint;

public class TestSocket extends WebSocketEndpoint {
  public static String lastBroadcast = null;

  public static void broadcast(String json) {
    lastBroadcast = json;
  }
}
