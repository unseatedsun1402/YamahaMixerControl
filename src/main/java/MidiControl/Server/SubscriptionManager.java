package MidiControl.Server;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.google.gson.JsonObject;

import MidiControl.Routing.WebSocketEndpoint;
import MidiControl.Server.EventStream.EventObject;
import jakarta.websocket.Session;

public class SubscriptionManager {

    // session -> set of contextIds
    private final Map<Session, Set<String>> sessionToContexts = new ConcurrentHashMap<>();

    // contextId -> set of sessions
    private final Map<String, Set<Session>> contextToSessions = new ConcurrentHashMap<>();

    private final Map<Session, String> sessionTypes = new ConcurrentHashMap<>();

    private static final Logger logger = Logger.getLogger(SubscriptionManager.class.getName());
    private static boolean debug = false;

    public static void enableDebug(){debug = true;}

    public void subscribe(Session session, String contextId) {
        sessionToContexts
            .computeIfAbsent(session, s -> ConcurrentHashMap.newKeySet())
            .add(contextId);

        contextToSessions
            .computeIfAbsent(contextId, c -> ConcurrentHashMap.newKeySet())
            .add(session);
    }

    public void registerSessionType(Session session, String type) {
        if (type == null) {logger.warning("No session type provided for "+session.getId()); return;};
        sessionTypes.put(session, type);
        logger.info(String.format("%s Session %s registered",type,session.getId()));
    }

    public void unsubscribe(Session session, String contextId) {
        Set<String> contexts = sessionToContexts.get(session);
        if (contexts != null) {
            contexts.remove(contextId);
            if (contexts.isEmpty()) {
                sessionToContexts.remove(session);
            }
        }

        Set<Session> sessions = contextToSessions.get(contextId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                contextToSessions.remove(contextId);
            }
        }
    }

    public void removeSession(Session session) {
        Set<String> contexts = sessionToContexts.remove(session);
        if (contexts != null) {
            for (String ctx : contexts) {
                Set<Session> sessions = contextToSessions.get(ctx);
                if (sessions != null) {
                    sessions.remove(session);
                    if (sessions.isEmpty()) {
                        contextToSessions.remove(ctx);
                    }
                }
            }
        }
        sessionTypes.remove(session);
        logger.info(String.format("Removed Session %s",session.getId()));
    }

    public Set<Session> getSubscribers(String contextId) {
        return contextToSessions.getOrDefault(contextId, Set.of());
    }

    public Set<Session> getAllSessions(){
        return sessionToContexts.keySet();
    }

    public String getSessionType(Session session) {
        return sessionTypes.getOrDefault(session, "unknown");
    }

    public void broadcastControlUpdate(String canonicalId, int value, Session origin) {

        String contextId = canonicalId.split("\\.")[0];

        Set<Session> sessions = getSubscribers(contextId);

        if (sessions.isEmpty()) {
            contextId = "channel." + canonicalId.split("\\.")[2];
            sessions = getSubscribers(contextId);
        }

        if (sessions.isEmpty()) {
            logger.warning(String.format("Cannot find any subscribers to %s",canonicalId));
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("contextId", contextId);
        payload.addProperty("canonicalId", canonicalId);
        payload.addProperty("value", value);

        JsonObject msg = EventObject.envelope(
            EventObject.Classification.EVENT,
            "control-update",
            Optional.empty(),
            payload
        );

        String json = msg.toString();

        for (Session s : sessions) {

            if (s == origin) continue;

            String type = getSessionType(s);

            if ("settings".equals(type)) {
                if (debug) logger.fine("Skipping control update for settings session " + s.getId());
                continue;
            }

            WebSocketEndpoint.send(s, json);

            if (debug)  logger.fine(String.format("Control update from  %s -> %s type=%s",canonicalId, origin.getId(), s.getId(), type));
        }
    }

}
