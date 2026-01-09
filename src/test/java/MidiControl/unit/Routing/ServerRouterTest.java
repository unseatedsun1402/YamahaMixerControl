package MidiControl.unit.Routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import MidiControl.MidiDeviceManager.MidiDeviceDTO;
import MidiControl.Mocks.FakeSession;
import MidiControl.Mocks.MockMidiIOManager;
import MidiControl.Mocks.MockMidiServer;
import MidiControl.Mocks.MockSubscriptionManager;
import MidiControl.Mocks.MockUiModelFactory;
import MidiControl.Server.ServerRouter;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexParser;
import MidiControl.UserInterface.UiModelService;
import MidiControl.UserInterface.DTO.UiModelDTO;

import java.util.ArrayList;
import java.util.List;

public class ServerRouterTest {

    private CanonicalRegistry makeMinimalRegistry() {
        List<SysexMapping> mappings = new ArrayList<>();
        SysexParser parser = new SysexParser(mappings);
        return new CanonicalRegistry(mappings, parser);
    }

    // Minimal UiModelService stub for tests
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
        return env;
    }

    @Test
    void testSetControlValue() {
        Env env = makeEnv();

        env.router.handleMessage(env.session, """
            {"type":"set-control-value","payload":{"canonicalId":"fader1","value":77}}
        """);

        String msg = env.session.lastSent;
        assertNotNull(msg, "Router did not send any response for set-control-value");
        // Depending on your real implementation, adjust these contains() checks:
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

        String msg = env.session.lastSent;
        assertNotNull(msg, "Router did not send midi-device-list response");
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
        String msg = env.session.lastSent;
        assertNotNull(msg, "Router did not send response for set-midi-device");
        assertTrue(msg.contains("\"status\":\"ok\""), msg);
    }

    @Test
    void testUnknownType() {
        Env env = makeEnv();

        env.router.handleMessage(env.session, """
            {"type":"does-not-exist","payload":{}}
        """);

        String msg = env.session.lastSent;
        assertNotNull(msg, "Router did not send error response for unknown type");
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

        router.handleMessage(session, """
            {"type":"unsubscribe-context","payload":{"contextId":"eq1"}}
        """);

        assertEquals("eq1", subs.lastUnsubscribed);
        String msg = session.lastSent;
        assertNotNull(msg, "Router did not send response for unsubscribe-context");
        assertTrue(msg.contains("\"status\":\"ok\""), msg);
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

        router.handleMessage(session, """
            {"type":"subscribe-context","payload":{"contextId":"eq1"}}
        """);

        assertEquals("eq1", subs.lastSubscribed);
        String msg = session.lastSent;
        assertNotNull(msg, "Router did not send response for subscribe-context");
        assertTrue(msg.contains("\"status\":\"ok\""), msg);
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

        router.handleMessage(session, """
            {"type":"get-ui-model","payload":{"contextId":"ch1"}}
        """);

        String msg = session.lastSent;
        assertNotNull(msg, "Router did not send ui-model response");
        assertTrue(msg.contains("\"type\":\"ui-model\""), msg);
        assertTrue(msg.contains("\"contextId\":\"ch1\""), msg);
    }
}