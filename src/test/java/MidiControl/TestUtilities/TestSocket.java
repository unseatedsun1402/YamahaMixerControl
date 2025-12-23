package MidiControl.TestUtilities;

import MidiControl.Socket;

public class TestSocket extends Socket {
  public static String lastBroadcast = null;

  public static void broadcast(String json) {
    lastBroadcast = json;
  }
}
