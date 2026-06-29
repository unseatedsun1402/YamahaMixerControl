package MidiControl.unit.Server.Protocol;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import MidiControl.Server.EventStream.EventObject;
import MidiControl.Server.Protocol.ServerEvent;
import MidiControl.Server.Protocol.ServerEventLevel;

public class ServerEventSerializerTest {

    @Test
    void serverEventSerialisesToEnvelope() {
        ServerEvent ev = new ServerEvent(
                Instant.ofEpochMilli(0),
                ServerEventLevel.ERROR,
                "PROTOCOL",
                "Unknown message type",
                null
        );

        JsonObject json = EventObject.fromServerEvent(ev);

        assertEquals("server-event", json.get("type").getAsString());

        JsonObject payload = json.getAsJsonObject("payload");
        assertEquals(0, payload.get("timestamp").getAsLong());
        assertEquals("ERROR", payload.get("level").getAsString());
        assertEquals("PROTOCOL", payload.get("category").getAsString());
        assertEquals("Unknown message type", payload.get("message").getAsString());
        assertFalse(payload.has("details"));
    }
}