package MidiControl.Routing;

import MidiControl.Server.MidiServer;
import MidiControl.Server.MidiServerListener;
import MidiControl.Server.ServerRouter;
import MidiControl.Server.SubscriptionManager;

import jakarta.websocket.OnOpen;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnClose;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@ServerEndpoint(value = "/endpoint")
public class WebSocketEndpoint {

    private static final Set<Session> sessions = ConcurrentHashMap.newKeySet();
    private static final Logger logger = Logger.getLogger(WebSocketEndpoint.class.getName());

    private MidiServer server;
    private ServerRouter router;
    private SubscriptionManager subscriptions;

    @OnOpen
    public void onOpen(Session session) {
        server = (MidiServer) MidiServerListener.CONTEXT.getAttribute("midiServer");
        router = server.getRouter();
        subscriptions = server.getSubscriptionManager();

        System.out.println("Client connected: " + session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        router.handleMessage(session, message);
    }

    @OnClose
    public void onClose(Session session) {
        subscriptions.removeSession(session);
    }


    // Sending helpers
    public static void send(Session session, String message) {
        try {
            if (session.isOpen()) {
                session.getBasicRemote().sendText(message);
            }
        } catch (Exception e) {
            logger.warning("Failed to send message: " + e.getMessage());
        }
    }

    public static void broadcast(String message) {
        for (Session s : sessions) {
            send(s, message);
        }
    }
}