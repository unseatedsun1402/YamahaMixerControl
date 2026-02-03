package MidiControl.unit.Routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.MidiDeviceManager.MidiDeviceDTO;
import MidiControl.Mocks.FakeSession;
import MidiControl.Mocks.MockMidiIOManager;
import MidiControl.Mocks.MockMidiServer;
import MidiControl.Mocks.MockSubscriptionManager;
import MidiControl.Routing.WebSocketEndpoint;
import MidiControl.Server.ServerRouter;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;
import MidiControl.UserInterface.UiModelService;
import MidiControl.UserInterface.DTO.UiModelDTO;

public class ServerRouterTest {
    
    public static void awaitWebSocket(FakeSession session) {
        for (int i = 0; i < 10 && session.lastSent == null; i++) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private CanonicalRegistry makeMinimalRegistry() {
        List<SysexMapping> mappings =
            SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");
        SysexParser parser = new SysexParser(mappings);
        return new CanonicalRegistry(mappings, parser);
    }

    // Minimal UiModelService stub
    private static class MockUiModelService implements UiModelService {
        @Override
        public UiModelDTO buildUiModel(String contextId, String uiType) {
            UiModelDTO dto = new UiModelDTO();
            dto.contextId = contextId;
            dto.controls = List.of();
            return dto;
        }
    }

    private static class Env {
        CanonicalRegistry registry;
        MockMidiServer server;
        MockMidiIOManager io;
        ServerRouter router;
        FakeSession session;
    }

    private Env makeEnv() {
        Env env = new Env();
        env.registry = makeMinimalRegistry();
        env.server = new MockMidiServer(env.registry);
        env.io = new MockMidiIOManager(env.server);
        env.server.setMockIo(env.io);

        env.router = new ServerRouter(
            new MockUiModelService(),
            env.server.getSubscriptionManager(),
            env.registry,
            env.io
        );

        env.session = new FakeSession("1");

        // CRITICAL: Register FakeSession with WebSocketEndpoint (AsyncRemote ready)
        WebSocketEndpoint endpoint = new WebSocketEndpoint();
        endpoint.setServerForTests(env.server);
        endpoint.onOpen(env.session);

        // Also attach FakeSession’s AsyncRemote to router implicit send path
        env.session.setRemoteSender(text -> env.session.lastSent = text);

        return env;
    }

    // -------------------------------------------------------------------------
    // TESTS
    // -------------------------------------------------------------------------

    @Test
    void testSetControlValue() {
        Env env = makeEnv();

        env.router.handleMessage(env.session, """
            {"type":"set-control-value","payload":{"canonicalId":"fader1","value":77}}
        """);
        awaitWebSocket(env.session);
        String msg = env.session.lastSent;
        assertNotNull(msg);
        assertTrue(msg.contains("\"type\""), msg);
        assertTrue(msg.contains("\"status\""), msg);
    }

    @Test
    void testListMidiDevices() {
        Env env = makeEnv();

        MidiDeviceDTO d1 = new MidiDeviceDTO();
        d1.id = "0";
        d1.name = "DeviceA";
        env.io.devices.add(d1);

        MidiDeviceDTO d2 = new MidiDeviceDTO();
        d2.id = "1";
        d2.name = "DeviceB";
        env.io.devices.add(d2);
        
        env.router.handleMessage(env.session, """
            {"type":"list-midi-devices","payload":{}}
        """);

        awaitWebSocket(env.session);
        String msg = env.session.lastSent;
        assertNotNull(msg);
        assertTrue(msg.contains("DeviceA"), msg);
        assertTrue(msg.contains("DeviceB"), msg);
        assertTrue(msg.contains("\"type\":\"midi-device-list\""), msg);
    }

    @Test
    void testSetMidiDeviceSuccess() {
        Env env = makeEnv();
        env.io.setResult = true;
        env.router.handleMessage(env.session, """
            {"type":"set-midi-device","payload":{"deviceId":3}}
        """);

        assertEquals(3, env.io.lastSetIndex);
        awaitWebSocket(env.session);
        String msg = env.session.lastSent;
        assertNotNull(msg);
        assertTrue(msg.contains("\"status\":\"ok\""), msg);
    }

    @Test
    void testUnknownType() {
        Env env = makeEnv();

        env.router.handleMessage(env.session, """
            {"type":"does-not-exist","payload":{}}
        """);
        awaitWebSocket(env.session);
        String msg = env.session.lastSent;
        assertNotNull(msg);
        assertTrue(msg.contains("\"type\":\"error\""), msg);
        assertTrue(msg.contains("UNKNOWN_TYPE"), msg);
    }

    @Test
    void testUnsubscribeContext() {
        CanonicalRegistry registry = makeMinimalRegistry();
        MockMidiServer server = new MockMidiServer(registry);
        MockMidiIOManager io = new MockMidiIOManager(server);
        server.setMockIo(io);

        MockSubscriptionManager subs = new MockSubscriptionManager();

        ServerRouter router = new ServerRouter(
            new MockUiModelService(),
            subs,
            registry,
            io
        );

        FakeSession session = new FakeSession("1");

        // Register FakeSession with endpoint
        WebSocketEndpoint endpoint = new WebSocketEndpoint();
        endpoint.setServerForTests(server);
        endpoint.onOpen(session);

        session.setRemoteSender(text -> session.lastSent = text);
        router.handleMessage(session, """
            {"type":"unsubscribe-context","payload":{"contextId":"eq1"}}
        """);
        awaitWebSocket(session);
        assertEquals("eq1", subs.lastUnsubscribed);
        assertNotNull(session.lastSent);
        assertTrue(session.lastSent.contains("\"status\":\"ok\""));
    }

    @Test
    void testSubscribeContext() {
        CanonicalRegistry registry = makeMinimalRegistry();
        MockMidiServer server = new MockMidiServer(registry);
        MockMidiIOManager io = new MockMidiIOManager(server);
        server.setMockIo(io);

        MockSubscriptionManager subs = new MockSubscriptionManager();

        ServerRouter router = new ServerRouter(
            new MockUiModelService(),
            subs,
            registry,
            io
        );

        FakeSession session = new FakeSession("1");

        WebSocketEndpoint endpoint = new WebSocketEndpoint();
        endpoint.setServerForTests(server);
        endpoint.onOpen(session);

        session.setRemoteSender(text -> session.lastSent = text);

        router.handleMessage(session, """
            {"type":"subscribe-context","payload":{"contextId":"eq1"}}
        """);

        assertEquals("eq1", subs.lastSubscribed);
        assertNotNull(session.lastSent);
        assertTrue(session.lastSent.contains("\"status\":\"ok\""));
    }

    @Test
    void testGetUiModel() {
        CanonicalRegistry registry = makeMinimalRegistry();
        MockMidiServer server = new MockMidiServer(registry);
        MockMidiIOManager io = new MockMidiIOManager(server);
        server.setMockIo(io);

        ServerRouter router = new ServerRouter(
            new MockUiModelService(),
            new MockSubscriptionManager(),
            registry,
            io
        );

        FakeSession session = new FakeSession("1");

        WebSocketEndpoint endpoint = new WebSocketEndpoint();
        endpoint.setServerForTests(server);
        endpoint.onOpen(session);

        session.setRemoteSender(text -> session.lastSent = text);
        
        router.handleMessage(session, """
            {"type":"get-ui-model","payload":{"contextId":"ch1"}}
        """);
        awaitWebSocket(session);
        assertNotNull(session.lastSent);
        assertTrue(session.lastSent.contains("\"type\":\"ui-model\""), session.lastSent);
        assertTrue(session.lastSent.contains("\"contextId\":\"ch1\""), session.lastSent);
    }

    @Test
    void testUpdateSettingChangesMappings() {
        List<SysexMapping> mappings =
            SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");

        CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));
        assertNotNull(registry.getGroup("kAUXToMatrix"));
        assertNull(registry.getGroup("kMixToMatrix"));

        MockMidiServer server = new MockMidiServer(registry);
        MockMidiIOManager io = new MockMidiIOManager(server);
        server.setMockIo(io);

        ServerRouter router = new ServerRouter(
            new MockUiModelService(),
            new MockSubscriptionManager(),
            registry,
            io
        );

        FakeSession session = new FakeSession("1");

        WebSocketEndpoint endpoint = new WebSocketEndpoint();
        endpoint.setServerForTests(server);
        endpoint.onOpen(session);

        session.setRemoteSender(text -> session.lastSent = text);

        router.handleMessage(session, """
            {"type":"apply-midi-settings","requestId":"req-1",
             "payload":{"inputDeviceId":0,"outputDeviceId":0,"consoleType":"YAMAHA_M7CL"}}
        """);

        assertNull(registry.getGroup("kAUXToMatrix"));
        assertNotNull(registry.getGroup("kMixToMatrix"));
    }
}