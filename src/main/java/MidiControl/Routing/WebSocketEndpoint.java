package MidiControl.Routing;

import MidiControl.ContextModel.BankContext;
import MidiControl.Access.Authenticate;
import MidiControl.Access.PasswordManager;
import MidiControl.Server.EventStream.EventObject;
import MidiControl.Server.EventStream.ServerResponses;
import MidiControl.Server.Protocol.NotifyClients;
import MidiControl.Server.Protocol.ServerEvent;
import MidiControl.Server.MidiServer;
import MidiControl.Server.MidiServerListener;
import MidiControl.Server.SubscriptionManager;
import MidiControl.UserInterface.UiBankFactory;
import MidiControl.UserInterface.DTO.UiBankDTO;

import jakarta.websocket.OnOpen;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnClose;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@ServerEndpoint(value = "/endpoint")
public class WebSocketEndpoint{

    private static final Set<Session> sessions = ConcurrentHashMap.newKeySet();
    private static final Map<Session, ConnectionState> connectionStates = new ConcurrentHashMap<>();
    private static final Map<Session, OutboundDeliveryState> outboundDeliveryStates =
            new ConcurrentHashMap<>();
    private static final Logger logger = Logger.getLogger(WebSocketEndpoint.class.getName());
    private static final Gson gson = new Gson();
    private static boolean debug = false;
    private static final long TIMEOUT = 3000;
    private static final int MAX_PENDING_MESSAGES = 256;
    public static String lastSent;

    private MidiServer server;
    private SubscriptionManager subscriptions;
    private static final Map<String, Long> logTimes =
        new ConcurrentHashMap<>();

    private static final long LOG_DEBOUNCE_MS = 5000;
    private static final int MAX_AUTHENTICATION_ATTEMPTS = 5;

    private static final class ConnectionState {
        private final boolean remote;
        private final String salt;
        private final String passwordHash;
        private volatile boolean authenticated;
        private int failedAttempts;

        private ConnectionState(boolean remote, String salt, String passwordHash, boolean authenticated) {
            this.remote = remote;
            this.salt = salt;
            this.passwordHash = passwordHash;
            this.authenticated = authenticated;
        }
    }

    private static final class OutboundDeliveryState {
        private final ArrayDeque<String> pendingMessages = new ArrayDeque<>();
        private String pendingMeterMessage;
        private boolean sending;
        private boolean failed;
    }

    private static void warningDebounced(String key, String message) {

        long now = System.currentTimeMillis();

        Long last = logTimes.get(key);

        if (last != null &&
            (now - last) < LOG_DEBOUNCE_MS) {
            return;
        }

        logTimes.put(key, now);

        logger.warning(message);
    }

    private static void errorDebounced(String key, String message) {

        long now = System.currentTimeMillis();

        Long last = logTimes.get(key);

        if (last != null &&
            (now - last) < LOG_DEBOUNCE_MS) {
            return;
        }

        logTimes.put(key, now);

        logger.severe(message);
    }

    public static void enabledebug(){
        debug = true;
    }

    private static String encode(String type, Object payload) {
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("type", type);
        envelope.put("payload", payload);
        return gson.toJson(envelope);
    }

    @OnOpen
    public void onOpen(Session session) {
        server = (MidiServer) MidiServerListener.CONTEXT.getAttribute("midiServer");
        subscriptions = server.getSubscriptionManager();

        String connectionId = null;
        try {
            java.util.List<String> values = session.getRequestParameterMap().get("connectionId");
            if (values != null && !values.isEmpty()) {
                connectionId = values.get(0);
            }
        } catch (UnsupportedOperationException exception) {
            logger.warning("WebSocket session does not expose handshake parameters; requiring authentication");
        }

        boolean remote = !isLoopbackAddress(WebSocketPeerAddressRegistry.consume(connectionId));
        if (!remote) {
            connectionStates.put(session, new ConnectionState(false, null, null, true));
            sessions.add(session);
            sendAuthenticationMessage(session, "auth-status", true, false);
            NotifyClients.publish(
                ServerEvent.info(
                    "ACCESS",
                    String.format("Session %s  is connected", session.getId()),
                    jsonDetails("Connection established")
                )
            );
            return;
        }

        try {
            Optional<String> passwordHash = PasswordManager.getStoredPasswordHash();
            if (passwordHash.isEmpty()) {
                connectionStates.put(session, new ConnectionState(true, null, null, true));
                sessions.add(session);
                sendAuthenticationMessage(session, "auth-status", true, false);
                NotifyClients.publish(
                    ServerEvent.info(
                        "ACCESS",
                        String.format("Session %s  is connected", session.getId()),
                        jsonDetails("Connection established")
                    )
                );
                return;
            }

            String salt = Authenticate.createSalt();
            connectionStates.put(session, new ConnectionState(true, salt, passwordHash.get(), false));
            sessions.add(session);
            JsonObject challenge = new JsonObject();
            challenge.addProperty("salt", salt);
            challenge.addProperty("sessionId", session.getId());
            sendAuthenticationMessage(session, "auth-challenge", challenge);
        } catch (java.io.IOException exception) {
            logger.severe("Unable to load system password hash for remote authentication: " + exception.getMessage());
            connectionStates.put(session, new ConnectionState(true, null, null, false));
            sessions.add(session);
            sendAuthenticationMessage(session, "auth-result", false, true);
            try {
                session.close();
                NotifyClients.publish(
                    ServerEvent.info(
                        "ACCESS",
                        String.format("Session %s  was rejected", session.getId()),
                        jsonDetails("Authentication failed")
                    )
                );
            } catch (java.io.IOException closeException) {
                logger.warning("Unable to close session after authentication setup failed: "
                        + closeException.getMessage());
            }
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            
            JsonElement root = JsonParser.parseString(message);

            if (!root.isJsonObject()) {
                errorDebounced("json-message","Invalid WebSocket message (not an object)");
                return;
            }

            JsonObject json = root.getAsJsonObject();

            String type = json.get("type").getAsString();

            if ("authenticate".equals(type)) {
                JsonElement payload = json.get("payload");
                handleAuthentication(session, payload != null && payload.isJsonObject()
                        ? payload.getAsJsonObject()
                        : null);
                return;
            }

            ConnectionState state = connectionStates.get(session);
            if (state == null || !state.authenticated) {
                return;
            }

            switch (type) {

                case "get-ui-bank": {       // mixing responsabilities, must move
                    String bankId = json.getAsJsonObject("payload").get("bankId").getAsString();
                    UiBankFactory bankFactory = server.getUiBankFactory();
                    BankContext bankCtx = server.getBankCatalog().getBank(bankId);
                    UiBankDTO bankDto = bankFactory.buildBank(bankId, bankCtx);
                    send(session, encode("ui-bank", bankDto));
                    break;
                }

                default:
                    try {
                        server.getServerRouter().handleMessage(session, message); // inefficent to get the serverrouter each message, swap to a dynamic reference
                    } catch (Exception e) {
                        String requestId = json.has("requestId") && json.get("requestId").isJsonPrimitive()
                                ? json.get("requestId").getAsString() : null;
                        logger.log(Level.WARNING, "Failed to process WebSocket request type=" + type
                                + " session=" + session.getId() + " requestId=" + requestId, e);
                        ServerResponses.error(session, requestId, "REQUEST_FAILED",
                                "The request failed; check the server log for details");
                    }

                    break;
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to process WebSocket message for session "
                    + session.getId(), e);
        }
    }

    private void handleAuthentication(Session session, JsonObject payload) {
        ConnectionState state = connectionStates.get(session);
        if (state == null || !state.remote || state.authenticated || state.salt == null) {
            sendAuthenticationMessage(session, "auth-result", false, false);
            return;
        }

        String token = payload != null && payload.has("token") && payload.get("token").isJsonPrimitive()
                ? payload.get("token").getAsString()
                : null;
        boolean accepted = Authenticate.verifyChallengeToken(
                state.salt,
                state.passwordHash,
                session.getId(),
                token);
        if (accepted) {
            state.authenticated = true;
            sendAuthenticationMessage(session, "auth-result", true, false);
            return;
        }

        state.failedAttempts++;
        sendAuthenticationMessage(session, "auth-result", false, false);
        if (state.failedAttempts >= MAX_AUTHENTICATION_ATTEMPTS) {
            try {
                session.close();
            } catch (java.io.IOException exception) {
                logger.warning("Unable to close session after failed authentication: " + exception.getMessage());
            }
        }
    }

    private static void sendAuthenticationMessage(
            Session session,
            String type,
            boolean accepted,
            boolean unavailable) {
        JsonObject payload = new JsonObject();
        payload.addProperty("authenticated", accepted);
        payload.addProperty("passwordRequired", !accepted);
        if (unavailable) {
            payload.addProperty("message", "System password is unavailable");
        }
        sendAuthenticationMessage(session, type, payload);
    }

    private static void sendAuthenticationMessage(Session session, String type, JsonObject payload) {
        String message = EventObject.envelope(
                EventObject.Classification.RESPONSE,
                type,
                Optional.empty(),
                payload).toString();
        queueMessage(session, message);
    }

    private static boolean isLoopbackAddress(String remoteAddress) {
        if (remoteAddress == null || remoteAddress.isBlank()) {
            return false;
        }
        try {
            return InetAddress.getByName(remoteAddress).isLoopbackAddress();
        } catch (UnknownHostException exception) {
            return false;
        }
    }

    @OnClose
    public void onClose(Session session) {
        subscriptions.removeSession(session);
        sessions.remove(session);
        connectionStates.remove(session);
        outboundDeliveryStates.remove(session);
        NotifyClients.publish(
            ServerEvent.info(
                "ACCESS",
                String.format("Session %s  was closed", session.getId()),
                jsonDetails("Connection closed")
            )
        );
    }

    public static void send(Session session, String message) {
        lastSent = message;
        if (session == null) {
            return;
        }
        ConnectionState state = connectionStates.get(session);
        if ((state == null && sessions.contains(session))
                || (state != null && !state.authenticated)) {
            return;
        }
        queueMessage(session, message);
    }

    public static void broadcast(String message) {
        lastSent = message;
        for (Session s : sessions) {
            ConnectionState state = connectionStates.get(s);
            if (state == null || state.authenticated) {
                queueMessage(s, message);
            }
        }
    }

    public static void broadcastMeterUpdates(String message) {
        lastSent = message;
        for (Session session : sessions) {
            ConnectionState state = connectionStates.get(session);
            if (state != null && !state.authenticated) {
                continue;
            }

            OutboundDeliveryState delivery = outboundDeliveryStates.computeIfAbsent(
                    session, ignored -> new OutboundDeliveryState());
            boolean startSending;
            synchronized (delivery) {
                if (delivery.failed) {
                    continue;
                }
                delivery.pendingMeterMessage = message;
                startSending = !delivery.sending;
                if (startSending) {
                    delivery.sending = true;
                }
            }
            if (startSending) {
                sendNextMessage(session, delivery);
            }
        }
    }

    private static void queueMessage(Session session, String message) {
        OutboundDeliveryState delivery = outboundDeliveryStates.computeIfAbsent(
                session, ignored -> new OutboundDeliveryState());
        boolean startSending;
        synchronized (delivery) {
            if (delivery.failed) {
                return;
            }
            if (delivery.pendingMessages.size() >= MAX_PENDING_MESSAGES) {
                delivery.failed = true;
                delivery.pendingMessages.clear();
                delivery.pendingMeterMessage = null;
                logger.severe("Stopping WebSocket sends for session " + session.getId()
                        + " because its outbound queue exceeded " + MAX_PENDING_MESSAGES
                        + " messages");
                return;
            }
            delivery.pendingMessages.addLast(message);
            startSending = !delivery.sending;
            if (startSending) {
                delivery.sending = true;
            }
        }
        if (startSending) {
            sendNextMessage(session, delivery);
        }
    }

    private static void sendNextMessage(Session session, OutboundDeliveryState delivery) {
        String message;
        synchronized (delivery) {
            if (delivery.failed) {
                delivery.sending = false;
                return;
            }
            message = delivery.pendingMessages.pollFirst();
            if (message == null) {
                message = delivery.pendingMeterMessage;
                delivery.pendingMeterMessage = null;
            }
            if (message == null) {
                delivery.sending = false;
                return;
            }
        }
        if (!session.isOpen()) {
            failDelivery(session, delivery, "WebSocket session is closed");
            return;
        }

        try {
            session.getAsyncRemote().setSendTimeout(TIMEOUT);
            session.getAsyncRemote().sendText(message, result -> {
                if (!result.isOK()) {
                    failDelivery(session, delivery,
                            "WebSocket send failed: " + result.getException());
                    return;
                }
                sendNextMessage(session, delivery);
            });
        } catch (RuntimeException exception) {
            failDelivery(session, delivery, "WebSocket send failed: " + exception.getMessage());
        }
    }

    private static void failDelivery(
            Session session,
            OutboundDeliveryState delivery,
            String reason) {
        synchronized (delivery) {
            delivery.failed = true;
            delivery.sending = false;
            delivery.pendingMessages.clear();
            delivery.pendingMeterMessage = null;
        }
        warningDebounced(
                "websocket-send-" + session.getId(),
                "Stopping WebSocket sends for session " + session.getId() + ": " + reason);
    }

    public static void addTestSession(Session testSession){
        sessions.add(testSession);
        connectionStates.put(testSession, new ConnectionState(false, null, null, true));
    }

    private JsonObject jsonDetails(String message) {
        JsonObject obj = new JsonObject();
        obj.addProperty("details", message);
        return obj;
    }
}
