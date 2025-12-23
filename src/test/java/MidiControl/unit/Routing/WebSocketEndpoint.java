package MidiControl.unit.Routing;

import jakarta.websocket.Session;

public class WebSocketEndpoint {

    public static String lastMessage;

    public static void send(Session session, String msg) {
        lastMessage = msg;
    }
}
