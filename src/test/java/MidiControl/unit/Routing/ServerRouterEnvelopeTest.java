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
import MidiControl.Access.Authenticate;
import MidiControl.Access.Authenticate.Role;
import MidiControl.Access.RoleCredentialStore;
import MidiControl.Routing.WebSocketEndpoint;
import MidiControl.MidiDeviceManager.MidiIOManager;
import MidiControl.Mocks.FakeSession;
import MidiControl.Mocks.MockCanonicalRegistry;
import MidiControl.Mocks.MockMidiIOManager;

import java.util.List;
import java.util.Map;

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
        session.getUserProperties().put(WebSocketEndpoint.CLIENT_IP_PROPERTY, "192.168.1.20");
        session.getUserProperties().put(
                WebSocketEndpoint.USER_AGENT_PROPERTY,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/123.0 Safari/537.36");

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
        Authenticate authenticate = new Authenticate(Map.of(
                Role.GUEST,
                new RoleCredentialStore.Credential(true, "", "", 0)));
        ServerRouter router = new ServerRouter(
            uiModels,
            subs,
            registry,
            ioManager,
            authenticate
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
        String fingerprintSalt = "test-fingerprint-salt";
        String token = authenticate.authenticateGuest(
                new Authenticate.ClientFingerprint(fingerprintSalt, "192.168.1.20", "Chrome", "Windows")).token();
        JsonObject authenticatedRequest = JsonParser.parseString(incoming).getAsJsonObject();
        JsonObject requestPayload = authenticatedRequest.getAsJsonObject("payload");
        requestPayload.addProperty("authToken", token);
        requestPayload.addProperty("fingerprintSalt", fingerprintSalt);
        router.handleMessage(session, authenticatedRequest.toString());

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
}
