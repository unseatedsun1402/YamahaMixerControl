package MidiControl.Server.EventStream;

import com.google.gson.JsonObject;
import jakarta.websocket.Session;
import MidiControl.Routing.WebSocketEndpoint;

import java.util.Optional;

public final class ServerEvents {

    private ServerEvents() {}

    /** Send to a single session */
    public static void send(Session session, String type, JsonObject payload) {
        WebSocketEndpoint.send(session, EventObject.envelope(EventObject.Classification.EVENT,type,Optional.empty(), payload).toString());
    }

    /** Broadcast to all sessions */
    public static void broadcast(String type, JsonObject payload) {
        String msg = EventObject.envelope(EventObject.Classification.EVENT,type,Optional.empty(), payload).toString();
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