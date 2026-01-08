package MidiControl.unit.Routing;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

import MidiControl.Server.ServerRouter;
import MidiControl.Server.SubscriptionManager;
import MidiControl.Server.MidiServer;
import MidiControl.UserInterface.UiModelFactory;
import MidiControl.UserInterface.DTO.UiModelDTO;
import MidiControl.Mocks.FakeSession;
import MidiControl.Mocks.MockCanonicalRegistry;
import MidiControl.Mocks.MockMidiServer;

import java.util.List;

class MinimalUiModelFactory extends UiModelFactory {

    private final UiModelDTO model;

    public MinimalUiModelFactory(UiModelDTO model) {
        super(null, null, null, null);
        this.model = model;
    }

    @Override
    public UiModelDTO buildUiModel(String contextId) {
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

        MinimalUiModelFactory factory = new MinimalUiModelFactory(fakeModel);
        MockMidiServer midi = new MockMidiServer(new MockCanonicalRegistry());

        ServerRouter router = new ServerRouter(factory, midi, new SubscriptionManager());

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
        assertTrue(payload.has("model"), "Expected model to be present in payload");

        JsonObject modelJson = payload.getAsJsonObject("model");

        // Validate DTO structure
        assertEquals("channel.1", modelJson.get("contextId").getAsString());
        assertTrue(modelJson.has("controls"));
        assertTrue(modelJson.getAsJsonArray("controls").isEmpty());
    }
}