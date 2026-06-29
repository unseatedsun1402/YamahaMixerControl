package MidiControl.Server.Protocol;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.time.Instant;

/**
 * Serialises ServerEvent into the standard envelope:
 * { "type": "server-event", "payload": { ... } }
 */
public final class ServerEventSerializer {

    private ServerEventSerializer() {}

    public static ServerEvent simple(ServerEventLevel level, String category, String message) {
        return new ServerEvent(Instant.now(), level, category, message, null);
    }
    
    public static ServerEvent fromThrowable(
            Throwable t,
            ServerEventLevel level,
            String category,
            String message
    ) {
        JsonObject details = new JsonObject();
        details.addProperty("exceptionType", t.getClass().getName());
        details.addProperty("exceptionMessage", t.getMessage());

        JsonArray stack = new JsonArray();
        for (StackTraceElement el : t.getStackTrace()) {
            stack.add(el.toString());
        }
        details.add("stackTrace", stack);

        return new ServerEvent(
                Instant.now(),
                level,
                category,
                message,
                details
        );
    }

    public static ServerEvent fatal(Throwable t, String category, String message) {
        return fromThrowable(
                t,
                ServerEventLevel.ERROR,
                category,
                message
        );
    }
}