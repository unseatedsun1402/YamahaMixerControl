package MidiControl.Routing;

import MidiControl.ContextModel.BankContext;
import MidiControl.Access.Authenticate;
import MidiControl.Access.PasswordManager;
import MidiControl.Server.EventStream.EventObject;
import MidiControl.Server.MidiServer;
import MidiControl.Server.MidiServerListener;
import MidiControl.Server.SubscriptionManager;
import MidiControl.UserInterface.UiBankFactory;
import MidiControl.UserInterface.DTO.UiBankDTO;

import jakarta.websocket.OnOpen;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnClose;
import jakarta.websocket.SendHandler;
import jakarta.websocket.SendResult;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@ServerEndpoint(value = "/endpoint")
public class WebSocketEndpoint{

    private static final Set<Session> sessions = ConcurrentHashMap.newKeySet();
    private static final Map<Session, ConnectionState> connectionStates = new ConcurrentHashMap<>();
    private static final Map<Session, MeterDeliveryState> meterDeliveryStates =
            new ConcurrentHashMap<>();
    private static final Logger logger = Logger.getLogger(WebSocketEndpoint.class.getName());
    private static final Gson gson = new Gson();
    private static boolean debug = false;
    private static final long TIMEOUT = 3000;
    public static String lastSent;

    private MidiServer server;
    private SubscriptionManager subscriptions;
    private static Map<Integer,Boolean> sessionLocks = new ConcurrentHashMap<>();

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

    private static final class MeterDeliveryState {
        private final AtomicReference<String> pendingMessage = new AtomicReference<>();
        private final AtomicBoolean sending = new AtomicBoolean();
        private volatile boolean failed;
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
            return;
        }

        try {
            Optional<String> passwordHash = PasswordManager.getStoredPasswordHash();
            if (passwordHash.isEmpty()) {
                connectionStates.put(session, new ConnectionState(true, null, null, true));
                sessions.add(session);
                sendAuthenticationMessage(session, "auth-status", true, false);
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
                        warningDebounced(
                            "process-message",
                            "Failed to process message"
                        );
                    }

                    break;
            }

        } catch (Exception e) {
            logger.severe("Failed to process message: " + e.getMessage());
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
        try {
            sendWithTimeout(session, message);
        } catch (Exception exception) {
            logger.warning("Failed to send authentication response: " + exception.getMessage());
        }
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
        meterDeliveryStates.remove(session);
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
        try {
            sendWithTimeout(session,message);
            }
        catch (Exception e) {
            logger.warning("Failed to send message: " + e.getMessage());
        }
    }

    public static void broadcast(String message) {
        lastSent = message;
        for (Session s : sessions) {
            ConnectionState state = connectionStates.get(s);
            if (state == null || state.authenticated) {
                sendWithTimeout(s,message);
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

            MeterDeliveryState delivery = meterDeliveryStates.computeIfAbsent(
                    session, ignored -> new MeterDeliveryState());
            if (!delivery.failed) {
                delivery.pendingMessage.set(message);
                sendNextMeterUpdate(session, delivery);
            }
        }
    }

    private static void sendNextMeterUpdate(Session session, MeterDeliveryState delivery) {
        if (delivery.failed || !session.isOpen()
                || !delivery.sending.compareAndSet(false, true)) {
            return;
        }

        String message = delivery.pendingMessage.getAndSet(null);
        if (message == null) {
            delivery.sending.set(false);
            if (delivery.pendingMessage.get() != null) {
                sendNextMeterUpdate(session, delivery);
            }
            return;
        }

        try {
            session.getAsyncRemote().setSendTimeout(TIMEOUT);
            session.getAsyncRemote().sendText(message, new SendHandler() {
                @Override
                public void onResult(SendResult result) {
                    delivery.sending.set(false);
                    if (!result.isOK()) {
                        delivery.failed = true;
                        warningDebounced(
                                "meter-send-" + session.getId(),
                                "Stopping meter updates for an unresponsive WebSocket session "
                                        + session.getId() + ": " + result.getException());
                        return;
                    }
                    if (delivery.pendingMessage.get() != null) {
                        sendNextMeterUpdate(session, delivery);
                    }
                }
            });
        } catch (RuntimeException exception) {
            delivery.sending.set(false);
            delivery.failed = true;
            warningDebounced(
                    "meter-send-" + session.getId(),
                    "Stopping meter updates for an unresponsive WebSocket session "
                            + session.getId() + ": " + exception.getMessage());
        }
    }

    private static void sendWithTimeout(Session session, String message){
        long timeout = System.currentTimeMillis() + TIMEOUT;
        while (System.currentTimeMillis()<timeout){
            if (session.isOpen()) {
                if(!lockSession(session)) continue;
                try {
                    session.getBasicRemote().sendText(message);
                    Thread.sleep(0, 100);
                    unlockSession(session);
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                unlockSession(session);
            }
        }
        logger.warning("Could not send message before timeout "+message);
    }

    private static boolean lockSession(Session session){
         return (sessionLocks.putIfAbsent(session.hashCode(), false) == null ? true : false);
    }

    private static boolean unlockSession(Session session){
        return sessionLocks.remove(session.hashCode());
    }

    public static void addTestSession(Session testSession){
        sessions.add(testSession);
        connectionStates.put(testSession, new ConnectionState(false, null, null, true));
    }
}
