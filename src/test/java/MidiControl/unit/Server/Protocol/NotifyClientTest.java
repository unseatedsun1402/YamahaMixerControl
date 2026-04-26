package MidiControl.unit.Server.Protocol;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import MidiControl.Mocks.FakeSession;
import MidiControl.Routing.WebSocketEndpoint;
import MidiControl.Server.Protocol.NotifyClients;
import MidiControl.Server.Protocol.ServerEvent;
import MidiControl.Server.Protocol.ServerEventLevel;
import MidiControl.Server.Protocol.ServerEventSerializer;

public class NotifyClientTest {
    @Test
    public void testPublishServerEventReachesSession(){
        FakeSession testSession = new FakeSession("test");
        WebSocketEndpoint.addTestSession(testSession);
        ServerEvent testEvent = new ServerEvent(Instant.EPOCH, ServerEventLevel.INFO, null, null, null);
        NotifyClients.publish(testEvent);
        assertEquals(testSession.lastSent, ServerEventSerializer.toJsonString(testEvent));
    }

    @Test
    public void testClassInstatiatesNotifyClients(){
        assertDoesNotThrow(() -> new NotifyClients());
    }
}
