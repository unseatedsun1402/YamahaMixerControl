package MidiControl.unit.Routing;

import MidiControl.Mocks.FakeSession;
import MidiControl.Routing.WebSocketEndpoint;
import jakarta.websocket.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("stress")
class WebSocketDeliveryStressTest {

    @BeforeEach
    void clearEndpointState() throws Exception {
        clearMap("sessions");
        clearMap("connectionStates");
        clearMap("outboundDeliveryStates");
    }

    @Test
    void reportsMeterCoalescingAndReliableQueueLimitForStalledClient() throws Exception {
        FakeSession meterSession = stalledSession("meter-stress");
        WebSocketEndpoint.addTestSession(meterSession);

        int meterUpdates = 10_000;
        for (int i = 0; i < meterUpdates; i++) {
            WebSocketEndpoint.broadcastMeterUpdates("meter-" + i);
        }

        assertEquals(1, meterSession.getSentMessages().size(),
                "Only the in-flight meter message should be sent while the client is stalled");
        meterSession.completeNextAsyncSend();
        assertEquals("meter-" + (meterUpdates - 1), meterSession.lastSent,
                "Only the latest coalesced meter update should be sent next");
        meterSession.completeNextAsyncSend();

        System.out.printf(
                "websocket,meterOffers=%d,inFlightBeforeDrain=1,messagesSentAfterDrain=%d%n",
                meterUpdates,
                meterSession.getSentMessages().size());

        FakeSession eventSession = stalledSession("event-stress");
        WebSocketEndpoint.addTestSession(eventSession);
        int reliableOffersBeforeLimit = 257;
        for (int i = 0; i < reliableOffersBeforeLimit; i++) {
            WebSocketEndpoint.send(eventSession, "reliable-" + i);
        }

        OutboundState state = outboundState(eventSession);
        assertEquals(reliableOffersBeforeLimit - 1, state.pendingMessages(),
                "The pending queue reaches its configured limit behind one in-flight send");
        WebSocketEndpoint.send(eventSession, "reliable-overflow");
        OutboundState overflowed = outboundState(eventSession);
        assertTrue(overflowed.failed(), "The delivery state should report the overflowing client");

        System.out.printf(
                "websocket,reliableOffers=%d,inFlight=1,pendingAtLimit=%d,overflowed=%s%n",
                reliableOffersBeforeLimit + 1,
                reliableOffersBeforeLimit - 1,
                overflowed.failed());
    }

    private static FakeSession stalledSession(String id) {
        FakeSession session = new FakeSession(id);
        session.setDeferAsyncSendCompletion(true);
        return session;
    }

    @SuppressWarnings("unchecked")
    private static void clearMap(String fieldName) throws Exception {
        Field field = WebSocketEndpoint.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        Object value = field.get(null);
        if (value instanceof Map<?, ?> map) {
            map.clear();
        } else if (value instanceof Set<?> set) {
            set.clear();
        }
    }

    private static OutboundState outboundState(Session session) throws Exception {
        Field statesField = WebSocketEndpoint.class.getDeclaredField("outboundDeliveryStates");
        statesField.setAccessible(true);
        Map<?, ?> states = (Map<?, ?>) statesField.get(null);
        Object delivery = states.get(session);

        Field messagesField = delivery.getClass().getDeclaredField("pendingMessages");
        messagesField.setAccessible(true);
        int pendingMessages = ((java.util.Queue<?>) messagesField.get(delivery)).size();

        Field failedField = delivery.getClass().getDeclaredField("failed");
        failedField.setAccessible(true);
        return new OutboundState(pendingMessages, failedField.getBoolean(delivery));
    }

    private record OutboundState(int pendingMessages, boolean failed) {}
}
