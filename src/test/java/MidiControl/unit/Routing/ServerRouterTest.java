package MidiControl.unit.Routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import MidiControl.MidiDeviceManager.MidiDeviceDTO;
import MidiControl.MidiDeviceManager.MidiSendEngine;
import MidiControl.Mocks.FakeSession;
import MidiControl.Mocks.MockMidiIOManager;
import MidiControl.Mocks.MockMidiOut;
import MidiControl.Mocks.MockMidiServer;
import MidiControl.Mocks.MockSubscriptionManager;
import MidiControl.Server.ServerRouter;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;
import MidiControl.UserInterface.UiModelService;
import MidiControl.UserInterface.DTO.UiModelDTO;

import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class ServerRouterTest {

    private CanonicalRegistry makeMinimalRegistry() {
        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");
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
        env.io.setMidiOutForTest(new MockMidiOut());
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
    
    private static JsonObject parseJson(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static String typeOf(String json) {
        return parseJson(json).get("type").getAsString();
    }

    private static JsonObject payloadOf(String json) {
        return parseJson(json).getAsJsonObject("payload");
    }



    @Test
    void testSetControlValue_UnregisteredCanonicalId_ReturnsError() {
        Env env = makeEnv();

        env.router.handleMessage(env.session, """
            {"requestId": 1,"type":"set-control-value","payload":{"canonicalId":"__definitely_missing__","value":77}}
        """);

        String msg = env.session.lastSent;
        assertNotNull(msg, "Router did not send any response for set-control-value");

        assertEquals("error", typeOf(msg), msg);

        JsonObject payload = payloadOf(msg);
        assertEquals("UNKNOWN_CANONICAL_ID", payload.get("code").getAsString(), msg);
        assertTrue(payload.get("message").getAsString().contains("__definitely_missing__"), msg);

        // Your error schema does not include "status"
        assertTrue(!payload.has("status"), msg);
    }

    @Test
    void testSetControlValue_RegisteredCanonicalId_ReturnsAck() {
        Env env = makeEnv();

        String existingId = env.registry.getAllInstances().stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("Registry has no instances"))
                .getCanonicalId();

        env.router.handleMessage(env.session, """
            {"requestId": 1,"type":"set-control-value","payload":{"canonicalId":"%s","value":77}}
        """.formatted(existingId));

        String msg = env.session.lastSent;
        assertNotNull(msg, "Router did not send any response for set-control-value");

        assertEquals("ack", typeOf(msg), msg);

        JsonObject payload = payloadOf(msg);
        assertEquals("ok", payload.get("status").getAsString(), msg);
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
            {"requestId": 1,"type":"list-midi-devices","payload":{}}
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
            {"requestId": 1,"type":"set-midi-device","payload":{"deviceId":3}}
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
            {"requestId": 1,"type":"does-not-exist","payload":{}}
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
            {"requestId": 1,"type":"unsubscribe-context","payload":{"contextId":"eq1"}}
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
            {"requestId": 1,"type":"subscribe-context","payload":{"contextId":"eq1"}}
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

    @Test
    void testUpdateSettingChangesMappings() {
        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");
        CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));
        assertNull(registry.getGroup("kMixToMatrix"));

        MockMidiServer server = new MockMidiServer(registry);
        MockMidiIOManager io = new MockMidiIOManager(server);
        server.setMockIo(io);
        io.sendEngine = new MidiSendEngine(io.getMidiOut(), 100,100);

        ServerRouter router = new ServerRouter(
            new MockUiModelService(),
            new MockSubscriptionManager(),
            registry,
            io
        );

        FakeSession session = new FakeSession("1");

        router.handleMessage(session, """
            {"type": "apply-midi-settings", "requestId": "req-1", "payload": {"inputDeviceId":0,"outputDeviceId":0,"consoleType":"YAMAHA_M7CL","safeprofile":"true","mainprofile":"false","highprofile":"false"}}
        """);

        //m7cl will have controls that 01v96 has not and vice versa
        assertNull(registry.getGroup("kAUXToMatrix"));
        assertNotNull(registry.getGroup("kMixToMatrix"));
    }
}