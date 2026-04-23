package MidiControl.unit.Routing;

import MidiControl.Mocks.FakeSession;
import MidiControl.Mocks.MockCanonicalRegistry;
import MidiControl.Mocks.MockMidiIOManager;
import MidiControl.Routing.WebSocketEndpoint;
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
import java.util.Set;

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

        Set<Session> sessions = getStaticSessions();
        sessions.add(s1);
        sessions.add(s2);

        WebSocketEndpoint.broadcast("All");

        assertEquals("All", WebSocketEndpoint.lastSent);
        assertEquals("All", s1.lastSent);
        assertEquals("All", s2.lastSent);
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
        FakeSession session = new FakeSession("abc");

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
        FakeSession session = new FakeSession("abc");

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
        FakeSession session = new FakeSession("s1");
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
        FakeSession session = new FakeSession("s1");
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
        assertDoesNotThrow(WebSocketEndpoint::enableDebug);
    }
}
