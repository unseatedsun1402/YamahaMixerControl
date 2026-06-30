package MidiControl.unit.Server.Protocol;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;


import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import MidiControl.Mocks.FakeSession;
import MidiControl.Routing.WebSocketEndpoint;
import MidiControl.Server.EventStream.EventObject;
import MidiControl.Server.Protocol.NotifyClients;
import MidiControl.Server.Protocol.ServerEvent;
import MidiControl.Server.Protocol.ServerEventLevel;

public class NotifyClientTest {
    @Test
    public void testPublishServerEventReachesSession(){
        FakeSession testSession = new FakeSession("test");
        WebSocketEndpoint.addTestSession(testSession);
        ServerEvent testEvent = new ServerEvent(Instant.EPOCH, ServerEventLevel.INFO, null, null, null);
        NotifyClients.publish(testEvent);
        assertEquals(new Gson().fromJson(testSession.lastSent,JsonObject.class).get("type"), EventObject.fromServerEvent(testEvent).get("type") );
        assertEquals(new Gson().fromJson(testSession.lastSent,JsonObject.class).get("category"), EventObject.fromServerEvent(testEvent).get("category") );
    }

    @Test
    public void testClassInstatiatesNotifyClients(){
        assertDoesNotThrow(() -> new NotifyClients());
    }
}
