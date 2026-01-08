package MidiControl.unit.Routing;
import MidiControl.Server.SubscriptionManager;
import MidiControl.Mocks.FakeSession;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class SubscriptionManagerBasicTest {

    @Test
    void testSubscribeAndRetrieve() {
        SubscriptionManager subs = new SubscriptionManager();

        FakeSession s1 = new FakeSession("A");
        FakeSession s2 = new FakeSession("B");

        subs.subscribe(s1, "channel.1");
        subs.subscribe(s2, "channel.1");


        Set<?> result = subs.getSubscribers("channel.1");

        assertEquals(2, result.size());
        assertTrue(result.contains(s1));
        assertTrue(result.contains(s2));
    }

    @Test
    void testUnsubscribe() {
        SubscriptionManager subs = new SubscriptionManager();

        FakeSession s1 = new FakeSession("A");
        FakeSession s2 = new FakeSession("B");

        subs.subscribe(s1, "channel.1");
        subs.subscribe(s2, "channel.1");


        subs.unsubscribe(s1,"channel.1");

        Set<?> result = subs.getSubscribers("channel.1");

        assertEquals(1, result.size());
        assertFalse(result.contains(s1));
        assertTrue(result.contains(s2));
    }

    @Test
    void testRemoveSessionFromAllContexts() {
        SubscriptionManager subs = new SubscriptionManager();

        FakeSession s = new FakeSession("A");

        subs.subscribe(s,"channel.1");
        subs.subscribe(s,"mix.3");
        subs.subscribe(s,"matrix.2");

        subs.removeSession(s);

        assertTrue(subs.getSubscribers("channel.1").isEmpty());
        assertTrue(subs.getSubscribers("mix.3").isEmpty());
        assertTrue(subs.getSubscribers("matrix.2").isEmpty());
    }

    @Test
    void testUnknownContextReturnsEmptySet() {
        SubscriptionManager subs = new SubscriptionManager();

        assertTrue(subs.getSubscribers("nonexistent").isEmpty());
    }

}