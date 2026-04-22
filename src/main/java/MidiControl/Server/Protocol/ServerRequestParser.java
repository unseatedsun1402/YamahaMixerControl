package MidiControl.Server.Protocol;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Parses the canonical message envelope. Not tied to any transport.
 */
public final class ServerRequestParser {

    private final Gson gson;

    public ServerRequestParser(Gson gson) {
        this.gson = gson;
    }

    private static final class Incoming {
        String type;
        String requestId;
        JsonElement payload;
    }

    public ServerRequest parse(String rawJson) {
        Incoming in = gson.fromJson(rawJson, Incoming.class);

        String type = (in != null) ? in.type : "";
        String requestId = (in != null) ? in.requestId : null;

        JsonObject payload = new JsonObject();
        if (in != null && in.payload != null && in.payload.isJsonObject()) {
            payload = in.payload.getAsJsonObject();
        }

        return ServerRequest.of(type, requestId, payload);
    }
}