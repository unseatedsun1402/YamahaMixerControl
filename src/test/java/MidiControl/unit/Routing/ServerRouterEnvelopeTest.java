package MidiControl.unit.Routing;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

import MidiControl.Server.ServerRouter;
import MidiControl.Server.SubscriptionManager;
import MidiControl.UserInterface.UiModelService;
import MidiControl.UserInterface.DTO.UiModelDTO;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.MidiDeviceManager.MidiIOManager;
import MidiControl.MidiDeviceManager.MidiSendEngine.ThroughputProfile;
import MidiControl.Mocks.FakeSession;
import MidiControl.Routing.WebSocketEndpoint;
import MidiControl.Mocks.MockCanonicalRegistry;
import MidiControl.Mocks.MockMidiIOManager;
import MidiControl.Routing.OutputRequestSender;
import MidiControl.Server.StateRehydrationService;
import MidiControl.Server.Rehydration.RehydrationManager;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

class MinimalUiModelService implements UiModelService {

    private final UiModelDTO model;

    public MinimalUiModelService(UiModelDTO model) {
        this.model = model;
    }

    @Override
    public UiModelDTO buildUiModel(String contextId, String uiType) {
        return model;
    }
}

public class ServerRouterEnvelopeTest {

    @Test
    void testGetUiModelEnvelopeIsHandledCorrectly() {

        // Capture outgoing WebSocket messages
        StringBuilder captured = new StringBuilder();
        FakeSession session = new FakeSession("test-session");

        // FakeSession AsyncRemote sender
        session.setRemoteSender(captured::append);

        // Build a minimal UiModelDTO
        UiModelDTO fakeModel = new UiModelDTO();
        fakeModel.contextId = "channel.1";
        fakeModel.controls = List.of();

        // UiModelService stub
        UiModelService uiModels = new MinimalUiModelService(fakeModel);

        // Minimal mocks for router dependencies
        CanonicalRegistry registry = new MockCanonicalRegistry();
        MidiIOManager ioManager = new MockMidiIOManager(null);
        SubscriptionManager subs = new SubscriptionManager();

        // Router under test
        ServerRouter router = new ServerRouter(
            uiModels,
            subs,
            registry,
            ioManager
        );

        // Incoming WebSocket message that router must handle
        String incoming = """
        {
          "type": "get-ui-model",
          "requestId": "req-123",
          "payload": {
              "contextId": "channel.1"
          }
        }
        """;

        // Act
        router.handleMessage(session, incoming);

        // Assert outbound envelope was captured
        assertFalse(captured.isEmpty(), "Expected a response to be sent");

        // Parse JSON envelope from captured WebSocket output
        JsonObject json = JsonParser.parseString(captured.toString()).getAsJsonObject();

        assertEquals("ui-model", json.get("type").getAsString());
        assertEquals("req-123", json.get("requestId").getAsString());

        JsonObject payload = json.getAsJsonObject("payload");
        assertEquals("channel.1", payload.get("contextId").getAsString());

        // Validate DTO structure
        assertTrue(payload.has("controls"));
        assertTrue(payload.getAsJsonArray("controls").isEmpty());

        System.out.println("Outbound Envelope: " + json);
    }

    @Test
    void meterKeepAliveSendsM7clRequestWithoutNarrowingClientTimestamp() {
        MockCanonicalRegistry registry = new MockCanonicalRegistry();
        registry.setDeskType("YAMAHA_M7CL");
        CapturingOutputRouter outputRouter = new CapturingOutputRouter();
        ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);
        RehydrationManager rehydrationManager =
                new RehydrationManager(outputRouter, registry, scheduler);
        try {
            MidiIOManager ioManager = new MockMidiIOManager(null);
            ServerRouter router = new ServerRouter(
                    new MinimalUiModelService(new UiModelDTO()),
                    new SubscriptionManager(),
                    registry,
                    ioManager);
            router.injectApp(new StateRehydrationService(rehydrationManager, ioManager));

            FakeSession session = new FakeSession("meter-session");
            String keepAlive = """
                    {"type":"meter-keep-alive","payload":{"timestamp":1793573046169}}
                    """;

            router.handleMessage(session, keepAlive);
            router.handleMessage(session, keepAlive);

            assertEquals(1, outputRouter.sendCount);
            assertArrayEquals(new byte[] {
                    (byte) 0xF0, 0x43, 0x30, 0x3E, 0x11, 0x21,
                    0x00, 0x00, 0x00, 0x00, 0x30, (byte) 0xF7
            }, outputRouter.lastSent);

            router.setRegistry(registry);
            router.handleMessage(session, keepAlive);
            assertEquals(2, outputRouter.sendCount,
                    "A registry reload must allow an immediate refresh");
        } finally {
            rehydrationManager.shutdown();
        }
    }

    @Test
    void clientCanSelectMeterCategorySourceAndChannelRange() {
        MockCanonicalRegistry registry = new MockCanonicalRegistry();
        registry.setDeskType("YAMAHA_M7CL");
        CapturingOutputRouter outputRouter = new CapturingOutputRouter();
        ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);
        RehydrationManager rehydrationManager =
                new RehydrationManager(outputRouter, registry, scheduler);
        try {
            MidiIOManager ioManager = new MockMidiIOManager(null);
            ServerRouter router = new ServerRouter(
                    new MinimalUiModelService(new UiModelDTO()),
                    new SubscriptionManager(),
                    registry,
                    ioManager);
            router.injectApp(new StateRehydrationService(rehydrationManager, ioManager));

            router.handleMessage(
                    new FakeSession("meter-mode-session"),
                    """
                    {"type":"request-meters","requestId":"meters-1","payload":{
                      "category":"INPUT","source":"IN_PRE_EQ","startChannel":1,"channelCount":1
                    }}
                    """);

            assertArrayEquals(new byte[] {
                    (byte) 0xF0, 0x43, 0x30, 0x3E, 0x11, 0x21,
                    0x00, 0x02, 0x01, 0x00, 0x01, (byte) 0xF7
            }, outputRouter.lastSent);
        } finally {
            rehydrationManager.shutdown();
        }
    }

    @Test
    void failedOutputStillSwitchesProfileAndReportsPartialFailure() throws Exception {
        var mappings = SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");
        CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));
        registry.setDeskType("YAMAHA_01V96I");
        MidiIOManager io = new MidiIOManager() {
            @Override public boolean trySetOutputDevice(int index) { return false; }
            @Override public boolean trySetInputDevice(int index) { return true; }
        };
        ServerRouter router = new ServerRouter(
                new MinimalUiModelService(new UiModelDTO()), new SubscriptionManager(), registry, io);
        RehydrationManager manager =
                new RehydrationManager(router.getOutputRouter(), registry, new ScheduledThreadPoolExecutor(1));
        registry.addReloadListener(manager::injectNewRegistry);
        FakeSession session = new FakeSession("failed-output");
        FakeSession observer = new FakeSession("profile-observer");
        WebSocketEndpoint.addTestSession(observer);
        try {
            router.injectApp(new StateRehydrationService(manager, io));
            router.handleMessage(session, """
                    {"type":"apply-midi-settings","requestId":"switch","payload":{
                      "inputDeviceId":5,"outputDeviceId":4,"consoleType":"YAMAHA_M7CL",
                      "safeprofile":false,"mainprofile":true,"highprofile":false
                    }}
                    """);

            assertEquals("YAMAHA_M7CL", registry.getDeskType());
            assertEquals(ThroughputProfile.FAST_USB, io.getThroughputProfile());
            JsonObject ack = JsonParser.parseString(session.lastSent).getAsJsonObject();
            assertEquals("switch", ack.get("requestId").getAsString());
            assertEquals("ok", ack.getAsJsonObject("payload").get("mapping_status").getAsString());
            assertEquals("ok", ack.getAsJsonObject("payload").get("input_status").getAsString());
            assertEquals("error", ack.getAsJsonObject("payload").get("output_status").getAsString());
            assertEquals("error", ack.getAsJsonObject("payload").get("midi_status").getAsString());
            assertTrue(observer.getSentMessages().stream()
                    .map(message -> JsonParser.parseString(message).getAsJsonObject())
                    .anyMatch(event -> "REGISTRY_CHANGED".equals(event.get("type").getAsString())
                            && "YAMAHA_M7CL".equals(event.getAsJsonObject("payload").get("profile").getAsString())));

            byte[] fader = {
                (byte) 0xF0, 0x43, 0x10, 0x3E, 0x11, 0x01, 0, 0x32, 0, 0,
                0, 0, 0, 0, 0, 6, 0x72, (byte) 0xF7
            };
            var control = registry.resolveSysex(fader);
            assertNotNull(control, "The logged M7CL fader response must resolve after the switch");
            assertEquals("kInputFader.kFader.0", control.getCanonicalId());
            assertEquals(882, control.getSysex().extractValue(fader));

            router.handleMessage(session, """
                    {"type":"save-midi-settings","requestId":"save","payload":{
                      "inputDeviceId":5,"outputDeviceId":4,"consoleType":"YAMAHA_M7CL",
                      "safeprofile":false,"mainprofile":true,"highprofile":false
                    }}
                    """);
            JsonObject save = JsonParser.parseString(session.lastSent).getAsJsonObject();
            assertEquals("save", save.get("requestId").getAsString());
            assertEquals("error", save.getAsJsonObject("payload").get("save_status").getAsString());
        } finally {
            var sessions = WebSocketEndpoint.class.getDeclaredField("sessions");
            sessions.setAccessible(true);
            ((java.util.Set<?>) sessions.get(null)).remove(observer);
            manager.shutdown();
            io.shutdown();
        }
    }

    private static final class CapturingOutputRouter implements OutputRequestSender {
        private byte[] lastSent;
        private int sendCount;

        @Override
        public void applyRequest(String canonicalId) {}

        @Override
        public void send(byte[] message) {
            lastSent = message.clone();
            sendCount++;
        }
    }
}
