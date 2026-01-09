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
import MidiControl.Mocks.FakeSession;
import MidiControl.Mocks.MockCanonicalRegistry;
import MidiControl.Mocks.MockMidiIOManager;

import java.util.List;

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
        session.setRemoteSender(captured::append);

        // Build a minimal UiModelDTO
        UiModelDTO fakeModel = new UiModelDTO();
        fakeModel.contextId = "channel.1";
        fakeModel.controls = List.of();

        // New: use UiModelService instead of UiModelFactory
        UiModelService uiModels = new MinimalUiModelService(fakeModel);

        // Minimal mocks for dependencies
        CanonicalRegistry registry = new MockCanonicalRegistry();
        MidiIOManager ioManager = new MockMidiIOManager(null);
        SubscriptionManager subs = new SubscriptionManager();

        // New constructor
        ServerRouter router = new ServerRouter(
            uiModels,
            subs,
            registry,
            ioManager
        );

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

        // Assert
        assertFalse(captured.isEmpty(), "Expected a response to be sent");

        JsonObject json = JsonParser.parseString(captured.toString()).getAsJsonObject();

        assertEquals("ui-model", json.get("type").getAsString());
        assertEquals("req-123", json.get("requestId").getAsString());

        JsonObject payload = json.getAsJsonObject("payload");
        assertEquals("channel.1", payload.get("contextId").getAsString());
        System.out.println(payload.toString());

        // Validate DTO structure
        assertEquals("channel.1", payload.get("contextId").getAsString());
        assertTrue(payload.has("controls"));
        assertTrue(payload.getAsJsonArray("controls").isEmpty());
    }

}