
package MidiControl.Server.Protocol;

import com.google.gson.JsonObject;
import java.time.Instant;

public record ServerEvent(
        Instant timestamp,
        ServerEventLevel level,
        String category,
        String message,
        JsonObject details
) {

    /* ---------- SIMPLE EVENTS ---------- */

    public static ServerEvent info(String category, String message) {
        return new ServerEvent(
                Instant.now(),
                ServerEventLevel.INFO,
                category,
                message,
                null
        );
    }

    public static ServerEvent warning(String category, String message) {
        return new ServerEvent(
                Instant.now(),
                ServerEventLevel.WARNING,
                category,
                message,
                null
        );
    }

    public static ServerEvent error(String category, String message) {
        return new ServerEvent(
                Instant.now(),
                ServerEventLevel.ERROR,
                category,
                message,
                null
        );
    }

    /* ---------- EVENTS WITH DETAILS ---------- */

    public static ServerEvent info(String category, String message, JsonObject details) {
        return new ServerEvent(
                Instant.now(),
                ServerEventLevel.INFO,
                category,
                message,
                details
        );
    }

    public static ServerEvent warning(String category, String message, JsonObject details) {
        return new ServerEvent(
                Instant.now(),
                ServerEventLevel.WARNING,
                category,
                message,
                details
        );
    }

    public static ServerEvent error(String category, String message, JsonObject details) {
        return new ServerEvent(
                Instant.now(),
                ServerEventLevel.ERROR,
                category,
                message,
                details
        );
    }

    public String getAsString(){
        return this.getAsString();
    }
}
