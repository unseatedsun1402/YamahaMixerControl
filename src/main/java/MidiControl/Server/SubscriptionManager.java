package MidiControl.Server;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.google.gson.JsonObject;

import MidiControl.Routing.WebSocketEndpoint;
import jakarta.websocket.Session;

public class SubscriptionManager {

    // session → set of contextIds
    private final Map<Session, Set<String>> sessionToContexts = new ConcurrentHashMap<>();

    // contextId → set of sessions
    private final Map<String, Set<Session>> contextToSessions = new ConcurrentHashMap<>();

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
    }

    public Set<Session> getSubscribers(String contextId) {
        return contextToSessions.getOrDefault(contextId, Set.of());
    }

    public Set<Session> getAllSessions(){
        return sessionToContexts.keySet();
    }

    public void broadcastControlUpdate(String canonicalId, int value) {
        String contextId = canonicalId.split("\\.")[0];

        Set<Session> sessions = getSubscribers(contextId);
        if (sessions.isEmpty()) {
            return;
        }

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "control-update");

        JsonObject payload = new JsonObject();
        payload.addProperty("contextId", contextId);
        payload.addProperty("canonicalId", canonicalId);
        payload.addProperty("value", value);
        msg.add("payload", payload);

        String json = msg.toString();

        for (Session s : sessions) {
            WebSocketEndpoint.send(s, json);
            logger.info("Gui control update from "+s.getId());
        }
    }

    public void broadcastControlUpdateWithout(String canonicalId, int value,Session session){
        String contextId = canonicalId.split("\\.")[0];
        if(debug)logger.info("Boardcasting control update: "+canonicalId+"->"+contextId+" val: "+value);

        Set<Session> sessions = getSubscribers(contextId);
        if(sessions.isEmpty()){contextId = "channel."+canonicalId.split("\\.")[2];
            sessions = getSubscribers(contextId);
        }
        
        if (sessions.isEmpty()) {
            logger.warning("Cannot find any subscribers to "+canonicalId);
            return;
        }

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "control-update");

        JsonObject payload = new JsonObject();
        payload.addProperty("contextId", contextId);
        payload.addProperty("canonicalId", canonicalId);
        payload.addProperty("value", value);
        msg.add("payload", payload);

        String json = msg.toString();

        for (Session s : sessions) {
            if(s.getId() != session.getId()){
                WebSocketEndpoint.send(s, json);
                if(debug)logger.fine("Gui control update from "+session.getId()+" sent to "+s.getId());
            }
        }
    }

}
