package MidiControl.Server;

import com.google.gson.JsonObject;
import jakarta.websocket.Session;
import MidiControl.Routing.WebSocketEndpoint;

/**
 * Small helper for creating/sending standard websocket responses.
 * Keeps ServerRouter free of repetitive JSON envelope code.
 */
public final class ServerResponses {

    private ServerResponses() { /* no instances */ }

    /** Send any JsonObject (serialised via toString()). */
    public static void send(Session session, JsonObject message) {
        WebSocketEndpoint.send(session, message.toString());
    }

    /** Create a standard envelope: { type, requestId?, payload? } */
    public static JsonObject envelope(String type, String requestId, JsonObject payload) {
        JsonObject root = new JsonObject();
        root.addProperty("type", type);
        if (requestId != null) root.addProperty("requestId", requestId);
        if (payload != null) root.add("payload", payload);
        return root;
    }

    /** Send ACK with payload { status: "ok" }. */
    public static void ackOk(Session session, String requestId) {
        JsonObject payload = new JsonObject();
        payload.addProperty("status", "ok");
        send(session, envelope("ack", requestId, payload));
    }

    /** Send ACK with payload { status: "ok"|"error" }. */
    public static void ackStatus(Session session, String requestId, boolean ok) {
        JsonObject payload = new JsonObject();
        payload.addProperty("status", ok ? "ok" : "error");
        send(session, envelope("ack", requestId, payload));
    }

    /**
     * Send ACK with a custom payload.
     * Useful when you need extra fields (e.g. apply-settings statuses).
     */
    public static void ack(Session session, String requestId, JsonObject payload) {
        send(session, envelope("ack", requestId, payload));
    }

    /** Send error message with standard payload. */
    public static void error(Session session, String requestId, String code, String message) {
        JsonObject payload = new JsonObject();
        payload.addProperty("code", code);
        payload.addProperty("message", message);
        send(session, envelope("error", requestId, payload));
    }
}
