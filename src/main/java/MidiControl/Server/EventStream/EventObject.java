package MidiControl.Server.EventStream;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import com.google.gson.JsonObject;

import MidiControl.Server.Protocol.ServerEvent;

public class EventObject {
    private static final AtomicLong SEQ = new AtomicLong(0);
    /** Create a standard envelope: { type, requestId?, payload? } */
    public static JsonObject envelope(
            Classification classification,   // "response" | "event" | "error"
            String type,
            Optional<String> requestId,
            JsonObject payload
    ) {
        JsonObject root = new JsonObject();

        root.addProperty("classification", classification.name());
        root.addProperty("type", type);
        root.addProperty("sequence", SEQ.incrementAndGet());
        root.addProperty("timestamp", System.currentTimeMillis());

        if(requestId.isPresent())root.addProperty("requestId", requestId.get());

        if (payload != null) {
            root.add("payload", payload);
        } else {
            root.add("payload", new JsonObject());
        }

        return root;
    }

    public static JsonObject fromServerEvent(ServerEvent event) {

        JsonObject payload = new JsonObject();
        payload.addProperty("timestamp", event.timestamp().toEpochMilli());
        payload.addProperty("level", event.level().name());
        payload.addProperty("category", event.category());
        payload.addProperty("message", event.message());

        if (event.details() != null) {
            payload.add("details", event.details());
        }

        return envelope(
                Classification.EVENT,
                "server-event",
                Optional.empty(),
                payload
        );
    }

    public static enum Classification {
        RESPONSE, EVENT, ERROR
    }
}
