package MidiControl.Server.Protocol;

import com.google.gson.JsonObject;

/**
 * Transport-agnostic envelope for messages coming into the server.
 * Works for WebSocket, HTTP, etc. as long as the JSON shape is the same.
 */
public record ServerRequest(String type, String requestId, JsonObject payload) {

    public static ServerRequest of(String type, String requestId, JsonObject payload) {
        return new ServerRequest(type != null ? type : "",
                requestId != null ? requestId : "",
                payload != null ? payload : new JsonObject());
    }

    public boolean hasType() {
        return type != null && !type.isBlank();
    }
}