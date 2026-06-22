package MidiControl.Server;

import com.google.gson.JsonObject;
import jakarta.websocket.Session;
import MidiControl.Routing.WebSocketEndpoint;

import java.util.Collection;

public final class ServerEvents {

    private ServerEvents() {}

    /** Basic event envelope: { type, payload } */
    public static JsonObject envelope(String type, JsonObject payload) {
        JsonObject root = new JsonObject();
        root.addProperty("type", type);
        if (payload != null) root.add("payload", payload);
        return root;
    }

    /** Send to a single session */
    public static void send(Session session, String type, JsonObject payload) {
        WebSocketEndpoint.send(session, envelope(type, payload).toString());
    }

    /** Broadcast to all sessions */
    public static void broadcast(String type, JsonObject payload) {
        String msg = envelope(type, payload).toString();
        WebSocketEndpoint.broadcast(msg);
    }

    /** Specific domain event: registry/profile changed */
    public static void registryChanged(String profile, String version) {
        JsonObject payload = new JsonObject();
        payload.addProperty("profile", profile);
        payload.addProperty("version", version);

        broadcast("REGISTRY_CHANGED", payload);
    }
}