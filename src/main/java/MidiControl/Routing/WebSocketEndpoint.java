package MidiControl.Routing;

import MidiControl.ContextModel.BankContext;
import MidiControl.Server.MidiServer;
import MidiControl.Server.MidiServerListener;
import MidiControl.Server.SubscriptionManager;
import MidiControl.UserInterface.UiBankFactory;
import MidiControl.UserInterface.DTO.UiBankDTO;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;

@ServerEndpoint(value = "/endpoint")
public class WebSocketEndpoint {

    private static final Logger logger = Logger.getLogger(WebSocketEndpoint.class.getName());
    private static final Gson gson = new Gson();
    private static boolean DEBUG = false;

    // Active sessions
    private static final Set<Session> sessions = ConcurrentHashMap.newKeySet();

    // NEW: Per-session sender queues
    private static final Map<Session, BlockingQueue<String>> outboundQueues = new ConcurrentHashMap<>();
    private static final Map<Session, Thread> senderThreads = new ConcurrentHashMap<>();

    // Instance state
    private MidiServer server = null;
    private SubscriptionManager subscriptions = null;

    // Test override flag
    private boolean testInjectedServer = false;

    // ----------------------------------------------------------
    // Test support
    // ----------------------------------------------------------

    public void setServerForTests(MidiServer server) {
        this.server = server;
        this.testInjectedServer = true;
    }

    public static void enableDebug() { DEBUG = true; }

    // ----------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------

    @OnOpen
    public void onOpen(Session session) {

        if (testInjectedServer) {
            // Already injected
        } else {
            if (MidiServerListener.CONTEXT == null) {
                throw new IllegalStateException("ServletContext is null - tests must use setServerForTests()");
            }
            this.server = (MidiServer) MidiServerListener.CONTEXT.getAttribute("midiServer");
        }

        if (server == null) {
            throw new IllegalStateException("MidiServer is null in WebSocketEndpoint.onOpen");
        }

        this.subscriptions = server.getSubscriptionManager();

        sessions.add(session);

        // Create queue
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        outboundQueues.put(session, queue);

        // Start sender thread
        Thread sender = new Thread(() -> runSender(session), "WS-Sender-" + session.getId());
        sender.setDaemon(true);
        sender.start();
        senderThreads.put(session, sender);

        logger.info("WebSocket connected: " + session.getId());
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);

        // Stop sender thread
        Thread t = senderThreads.remove(session);
        if (t != null) t.interrupt();

        outboundQueues.remove(session);

        if (subscriptions != null) {
            subscriptions.removeSession(session);
        }

        logger.info("WebSocket disconnected: " + session.getId());
    }

    // ----------------------------------------------------------
    // Dedicated per-session sender thread
    // ----------------------------------------------------------

    private void runSender(Session session) {
        BlockingQueue<String> queue = outboundQueues.get(session);

        try {
            while (session.isOpen()) {
                String msg = queue.take();  // wait for next message

                session.getAsyncRemote().sendText(msg, result -> {
                    if (!result.isOK()) {
                        Throwable ex = result.getException();
                        logger.warning("Async send failed for session " + session.getId()
                                + ": " + (ex != null ? ex.getMessage() : "unknown"));
                    }
                });
            }
        } catch (InterruptedException e) {
            logger.info("Sender thread exiting for session " + session.getId());
        }
    }

    // ----------------------------------------------------------
    // Incoming message routing
    // ----------------------------------------------------------

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            JsonElement root = JsonParser.parseString(message);

            if (!root.isJsonObject()) {
                logger.severe("Invalid JSON: " + message);
                return;
            }

            JsonObject json = root.getAsJsonObject();
            String type = json.get("type").getAsString();

            switch (type) {

                case "get-ui-bank" -> {
                    JsonObject payload = json.getAsJsonObject("payload");
                    String bankId = payload.get("bankId").getAsString();

                    UiBankFactory bankFactory = server.getUiBankFactory();
                    BankContext bctx = server.getBankCatalog().getBank(bankId);
                    UiBankDTO dto = bankFactory.buildBank(bankId, bctx);

                    send(session, encode("ui-bank", dto));
                }

                default -> {
                    server.getServerRouter().handleMessage(session, message);
                }
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to process message", e);
        }
    }

    // ----------------------------------------------------------
    // Outbound sending (via queue)
    // ----------------------------------------------------------

    private String encode(String type, Object payload) {
        Map<String, Object> env = new HashMap<>();
        env.put("type", type);
        env.put("payload", payload);
        return gson.toJson(env);
    }

    public static void send(Session session, String message) {
        BlockingQueue<String> q = outboundQueues.get(session);
        if (q != null && session.isOpen()) q.offer(message);
    }

    public static void broadcast(String message) {
        if (DEBUG) logger.info("Broadcasting: " + message);

        for (Session s : sessions) {
            if (s.isOpen()) {
                BlockingQueue<String> q = outboundQueues.get(s);
                if (q != null) q.offer(message);
            }
        }
    }
}