package MidiControl.Server.EventStream;

import java.util.Optional;

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

    /** Send ACK with payload { status: "ok" }. */
    public static void ackOk(Session session, String requestId) {
        JsonObject payload = new JsonObject();
        payload.addProperty("status", "ok");
        send(session, EventObject.envelope(EventObject.Classification.RESPONSE,"ack", Optional.of(requestId), payload));
    }

    /** Send ACK with payload { status: "ok"|"error" }. */
    public static void ackStatus(Session session, String requestId, boolean ok) {
        JsonObject payload = new JsonObject();
        payload.addProperty("status", ok ? "ok" : "error");
        send(session, EventObject.envelope(EventObject.Classification.RESPONSE,"ack", Optional.of(requestId), payload));
    }

    /**
     * Send ACK with a custom payload.
     * Useful when you need extra fields (e.g. apply-settings statuses).
     */
    public static void ack(Session session, String requestId, JsonObject payload) {
        send(session, EventObject.envelope(EventObject.Classification.RESPONSE,"ack", Optional.of(requestId), payload));
    }

    /** Send error message with standard payload. */
    public static void error(Session session, String requestId, String code, String message) {
        JsonObject payload = new JsonObject();
        payload.addProperty("code", code);
        payload.addProperty("message", message);
        send(session, EventObject.envelope(EventObject.Classification.ERROR,"error", Optional.of(requestId), payload));
    }
}
