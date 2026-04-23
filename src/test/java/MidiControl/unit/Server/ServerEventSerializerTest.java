package MidiControl.unit.Server;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import MidiControl.Server.Protocol.ServerEvent;
import MidiControl.Server.Protocol.ServerEventLevel;
import MidiControl.Server.Protocol.ServerEventSerializer;

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

        String json = ServerEventSerializer.toJsonString(ev);

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        assertEquals("server-event", root.get("type").getAsString());

        JsonObject payload = root.getAsJsonObject("payload");
        assertEquals(0, payload.get("timestamp").getAsLong());
        assertEquals("ERROR", payload.get("level").getAsString());
        assertEquals("PROTOCOL", payload.get("category").getAsString());
        assertEquals("Unknown message type", payload.get("message").getAsString());
        assertFalse(payload.has("details"));
    }
}