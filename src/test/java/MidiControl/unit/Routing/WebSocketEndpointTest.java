package MidiControl.unit.Routing;

import MidiControl.Mocks.FakeSession;
import MidiControl.Mocks.MockCanonicalRegistry;
import MidiControl.Mocks.MockMidiIOManager;
import MidiControl.Routing.WebSocketEndpoint;
import MidiControl.Routing.WebSocketPeerAddressRegistry;
import MidiControl.Server.MidiServer;
import MidiControl.Server.MidiServerListener;
import MidiControl.Server.ServerRouter;
import MidiControl.Server.SubscriptionManager;

import MidiControl.ContextModel.*;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.UserInterface.UiBankFactory;

import MidiControl.UserInterface.DTO.UiModelDTO;
import MidiControl.UserInterface.UiModelService;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import jakarta.websocket.Session;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketEndpointTest {

    @BeforeEach
    void resetStatics() throws Exception {
        WebSocketEndpoint.lastSent = null;
        clearStaticSessions();
    }

    @SuppressWarnings("unchecked")
    private static Set<Session> getStaticSessions() throws Exception {
        Field f = WebSocketEndpoint.class.getDeclaredField("sessions");
        f.setAccessible(true);
        return (Set<Session>) f.get(null);
    }

    private static void clearStaticSessions() throws Exception {
        getStaticSessions().clear();
        Field f = WebSocketEndpoint.class.getDeclaredField("connectionStates");
        f.setAccessible(true);
        ((Map<?, ?>) f.get(null)).clear();
        f = WebSocketEndpoint.class.getDeclaredField("outboundDeliveryStates");
        f.setAccessible(true);
        ((Map<?, ?>) f.get(null)).clear();
    }

    private static FakeSession localSession(String id) {
        FakeSession session = new FakeSession(id);
        String connectionId = UUID.randomUUID().toString();
        WebSocketPeerAddressRegistry.register(connectionId, "127.0.0.1");
        session.setRequestParameters(Map.of("connectionId", List.of(connectionId)));
        return session;
    }

    /** ContextDiscoveryEngine that returns a deterministic list (no real discovery). */
    private static ContextDiscoveryEngine fixedDiscovery(CanonicalRegistry reg, List<Context> contexts) {
        return new ContextDiscoveryEngine(reg) {
            @Override public List<Context> discoverContexts() { return contexts; }
        };
    }

    /** ServerRouter seam that records delegation calls. */
    private static class CapturingServerRouter extends ServerRouter {
        Session lastSession;
        String lastMessage;

        CapturingServerRouter(UiModelService uiModels, SubscriptionManager subs,
                              CanonicalRegistry registry, MockMidiIOManager io) {
            super(uiModels, subs, registry, io);
        }

        // This assumes ServerRouter has handleMessage(Session,String) (it’s called by WebSocketEndpoint).
        public void handleMessage(Session session, String message) {
            this.lastSession = session;
            this.lastMessage = message;
        }
    }

    /** MidiServer test double with overridden getters used by WebSocketEndpoint. */
    private static class WsTestServer extends MidiServer {
        private final SubscriptionManager subs;
        private final BankCatalog catalog;
        private final UiBankFactory bankFactory;
        private final ServerRouter router;

        WsTestServer(CanonicalRegistry reg,
                     SubscriptionManager subs,
                     BankCatalog catalog,
                     UiBankFactory bankFactory,
                     ServerRouter router) {
            super(reg); // uses MidiServer(CanonicalRegistry) constructor
            this.subs = subs;
            this.catalog = catalog;
            this.bankFactory = bankFactory;
            this.router = router;
        }

        @Override public SubscriptionManager getSubscriptionManager() { return subs; }
        @Override public BankCatalog getBankCatalog() { return catalog; }
        @Override public UiBankFactory getUiBankFactory() { return bankFactory; }
        @Override public ServerRouter getServerRouter() { return router; }
    }

    private static class MinimalUiModelService implements UiModelService {
        @Override public UiModelDTO buildUiModel(String contextId, String uiType) {
            UiModelDTO dto = new UiModelDTO();
            dto.contextId = contextId;
            dto.controls = List.of();
            return dto;
        }
    }

    // -------------------------
    // Tests for send/broadcast
    // -------------------------

    @Test
    void send_setsLastSent_evenIfSessionNull() {
        WebSocketEndpoint.send(null, "TestString");
        assertEquals("TestString", WebSocketEndpoint.lastSent);
    }

    @Test
    void send_deliversToSessionBasicRemote() {
        FakeSession session = new FakeSession("1");
        WebSocketEndpoint.send(session, "Hello");

        assertEquals("Hello", WebSocketEndpoint.lastSent);
        assertEquals("Hello", session.lastSent);
    }

    @Test
    void broadcast_deliversToAllSessionsInStaticSet() throws Exception {
        FakeSession s1 = new FakeSession("1");
        FakeSession s2 = new FakeSession("2");

        WebSocketEndpoint.addTestSession(s1);
        WebSocketEndpoint.addTestSession(s2);

        WebSocketEndpoint.broadcast("All");

        assertEquals("All", WebSocketEndpoint.lastSent);
        assertEquals("All", s1.lastSent);
        assertEquals("All", s2.lastSent);
    }

    @Test
    void concurrentSendsAreSerializedPerSession() throws Exception {
        FakeSession session = new FakeSession("serialized");
        session.setDeferAsyncSendCompletion(true);
        WebSocketEndpoint.addTestSession(session);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Thread first = new Thread(() -> sendAfterSignal(session, "first", ready, start));
        Thread second = new Thread(() -> sendAfterSignal(session, "second", ready, start));
        first.start();
        second.start();

        assertTrue(ready.await(1, TimeUnit.SECONDS));
        start.countDown();
        first.join(1000);
        second.join(1000);

        assertFalse(first.isAlive());
        assertFalse(second.isAlive());
        assertEquals(1, session.getSentMessages().size(),
                "Only one async write may be in flight for a session");

        session.completeNextAsyncSend();
        assertEquals(2, session.getSentMessages().size(),
                "The queued write should start after the first write completes");
        session.completeNextAsyncSend();
    }

    @Test
    void meterUpdatesCoalesceBehindReliableMessages() throws Exception {
        FakeSession session = new FakeSession("meter-coalesce");
        session.setDeferAsyncSendCompletion(true);
        WebSocketEndpoint.addTestSession(session);

        WebSocketEndpoint.broadcastMeterUpdates("meter-1");
        WebSocketEndpoint.broadcastMeterUpdates("meter-2");
        WebSocketEndpoint.broadcastMeterUpdates("meter-3");
        WebSocketEndpoint.broadcast("reliable");

        assertEquals(List.of("meter-1"), session.getSentMessages());
        session.completeNextAsyncSend();
        assertEquals(List.of("meter-1", "reliable"), session.getSentMessages());
        session.completeNextAsyncSend();
        assertEquals(List.of("meter-1", "reliable", "meter-3"), session.getSentMessages());
        session.completeNextAsyncSend();
    }

    private static void sendAfterSignal(
            FakeSession session,
            String message,
            CountDownLatch ready,
            CountDownLatch start) {
        ready.countDown();
        try {
            if (start.await(1, TimeUnit.SECONDS)) {
                WebSocketEndpoint.send(session, message);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    // -------------------------
    // onOpen / onClose lifecycle
    // -------------------------

    @Test
    void onOpen_addsSession_andInitialisesSubscriptionsFromContext() throws Exception {
        // Arrange: deterministic registry + bank setup
        CanonicalRegistry reg = new MockCanonicalRegistry();
        SubscriptionManager subs = new SubscriptionManager();

        BankCatalog catalog = new BankCatalog(); // includes "bank.inputs" out-of-the-box

        // Provide a deterministic discovery list
        List<Context> contexts = List.of(
                new Context("channel.10", "Channel 10", ContextType.CHANNEL, List.of(), List.of()),
                new Context("mix.0",     "Mix 0",      ContextType.MIX,     List.of(), List.of()),
                new Context("channel.2", "Channel 2",  ContextType.CHANNEL, List.of(), List.of()),
                new Context("channel.1", "Channel 1",  ContextType.CHANNEL, List.of(), List.of())
        );

        UiBankFactory bankFactory = new UiBankFactory(fixedDiscovery(reg, contexts), new MidiServer(reg));
        // Router not used in this test
        ServerRouter router = new CapturingServerRouter(new MinimalUiModelService(), subs, reg, new MockMidiIOManager(null));

        MidiServer server = new WsTestServer(reg, subs, catalog, bankFactory, router);

        // Put server into the ServletContext used by MidiServerListener
        MidiControl.Mocks.MockServletContext ctx = new MidiControl.Mocks.MockServletContext();
        ctx.setAttribute("midiServer", server);
        MidiServerListener.CONTEXT = ctx;

        WebSocketEndpoint endpoint = new WebSocketEndpoint();
        FakeSession session = localSession("abc");

        // Act
        endpoint.onOpen(session);

        // Assert: static set updated
        assertTrue(getStaticSessions().contains(session));
    }

    @Test
    void onClose_removesSession_fromSubscriptions_andStaticSet() throws Exception {
        CanonicalRegistry reg = new MockCanonicalRegistry();
        SubscriptionManager subs = new SubscriptionManager();

        BankCatalog catalog = new BankCatalog();
        UiBankFactory bankFactory = new UiBankFactory(fixedDiscovery(reg, List.of()), new MidiServer(reg));
        ServerRouter router = new CapturingServerRouter(new MinimalUiModelService(), subs, reg, new MockMidiIOManager(null));
        MidiServer server = new WsTestServer(reg, subs, catalog, bankFactory, router);

        MidiControl.Mocks.MockServletContext ctx = new MidiControl.Mocks.MockServletContext();
        ctx.setAttribute("midiServer", server);
        MidiServerListener.CONTEXT = ctx;

        WebSocketEndpoint endpoint = new WebSocketEndpoint();
        FakeSession session = localSession("abc");

        endpoint.onOpen(session);

        // subscribe session so removeSession has real work to do
        subs.subscribe(session, "channel.1");
        assertTrue(subs.getSubscribers("channel.1").contains(session));

        endpoint.onClose(session);

        assertFalse(getStaticSessions().contains(session));
        assertFalse(subs.getSubscribers("channel.1").contains(session));
    }

    // -------------------------
    // onMessage parsing + routing
    // -------------------------

    @Test
    void onMessage_invalidJson_doesNotThrow_andDoesNotSend() {
        WebSocketEndpoint endpoint = new WebSocketEndpoint();
        FakeSession session = new FakeSession("1");

        assertDoesNotThrow(() -> endpoint.onMessage("{not json", session));
        assertNull(WebSocketEndpoint.lastSent);
        assertNull(session.lastSent);
    }

    @Test
    void onMessage_nonObjectJson_isIgnored() {
        WebSocketEndpoint endpoint = new WebSocketEndpoint();
        FakeSession session = new FakeSession("1");

        endpoint.onMessage("\"just a string\"", session);

        assertNull(WebSocketEndpoint.lastSent);
        assertNull(session.lastSent);
    }

    @Test
    void onMessage_getUiBank_sendsUiBankEnvelope_withSortedFilteredContexts() throws Exception {
        // Arrange server in context
        CanonicalRegistry reg = new MockCanonicalRegistry();
        SubscriptionManager subs = new SubscriptionManager();

        // BankCatalog contains bank.inputs with a CHANNEL + "channel" prefix filter
        BankCatalog catalog = new BankCatalog();

        // Provide contexts deliberately out-of-order and with a non-matching type
        List<Context> contexts = List.of(
                new Context("channel.10", "Channel 10", ContextType.CHANNEL, List.of(), List.of()),
                new Context("mix.0",     "Mix 0",      ContextType.MIX,     List.of(), List.of()),
                new Context("channel.2", "Channel 2",  ContextType.CHANNEL, List.of(), List.of()),
                new Context("channel.1", "Channel 1",  ContextType.CHANNEL, List.of(), List.of())
        );

        // Build UiBankFactory using deterministic discovery engine
        MidiServer dummyForCtor = new MidiServer(reg);
        UiBankFactory bankFactory = new UiBankFactory(fixedDiscovery(reg, contexts), dummyForCtor);

        CapturingServerRouter router = new CapturingServerRouter(new MinimalUiModelService(), subs, reg, new MockMidiIOManager(null));
        MidiServer server = new WsTestServer(reg, subs, catalog, bankFactory, router);

        MidiControl.Mocks.MockServletContext ctx = new MidiControl.Mocks.MockServletContext();
        ctx.setAttribute("midiServer", server);
        MidiServerListener.CONTEXT = ctx;

        WebSocketEndpoint endpoint = new WebSocketEndpoint();
        FakeSession session = localSession("s1");
        endpoint.onOpen(session);

        String incoming = """
          {"type":"get-ui-bank","payload":{"bankId":"bank.inputs"}}
        """;

        // Act
        endpoint.onMessage(incoming, session);

        // Assert: envelope type == "ui-bank"
        assertNotNull(session.lastSent, "Expected a response to be sent");
        JsonObject env = JsonParser.parseString(session.lastSent).getAsJsonObject();
        assertEquals("ui-bank", env.get("type").getAsString());

        // payload.contexts should contain only channel.* and be sorted by numeric index
        JsonObject payload = env.getAsJsonObject("payload");
        JsonArray arr = payload.getAsJsonArray("contexts");

        assertEquals(List.of("channel.1", "channel.2", "channel.10"),
                List.of(arr.get(0).getAsString(), arr.get(1).getAsString(), arr.get(2).getAsString()));
    }

    @Test
    void onMessage_unknownType_delegatesToServerRouter_handleMessage() {
        CanonicalRegistry reg = new MockCanonicalRegistry();
        SubscriptionManager subs = new SubscriptionManager();
        BankCatalog catalog = new BankCatalog();
        UiBankFactory bankFactory = new UiBankFactory(fixedDiscovery(reg, List.of()), new MidiServer(reg));

        CapturingServerRouter router = new CapturingServerRouter(new MinimalUiModelService(), subs, reg, new MockMidiIOManager(null));
        MidiServer server = new WsTestServer(reg, subs, catalog, bankFactory, router);

        MidiControl.Mocks.MockServletContext ctx = new MidiControl.Mocks.MockServletContext();
        ctx.setAttribute("midiServer", server);
        MidiServerListener.CONTEXT = ctx;

        WebSocketEndpoint endpoint = new WebSocketEndpoint();
        FakeSession session = localSession("s1");
        endpoint.onOpen(session);

        String incoming = """
          {"type":"unknown","payload":{"x":1}}
        """;

        endpoint.onMessage(incoming, session);

        assertEquals(incoming, router.lastMessage);
        assertEquals(session, router.lastSession);
    }

    @Test
    void enableDebug_doesNotThrow() {
        assertDoesNotThrow(WebSocketEndpoint::enabledebug);
    }
}
