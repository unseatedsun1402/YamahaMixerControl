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
import MidiControl.Mocks.MockMidiServer;
import MidiControl.Routing.WebSocketEndpoint;

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
    void testGetUiModelEnvelopeIsHandledCorrectly() throws Exception {

        // Capture outgoing WS messages
        StringBuilder captured = new StringBuilder();
        FakeSession session = new FakeSession("test-session");
        session.setRemoteSender(captured::append);

        // Build a minimal UiModelDTO
        UiModelDTO fakeModel = new UiModelDTO();
        fakeModel.contextId = "channel.1";
        fakeModel.controls = List.of();

        UiModelService uiModels = new MinimalUiModelService(fakeModel);

        CanonicalRegistry registry = new MockCanonicalRegistry();
        MidiIOManager ioManager = new MockMidiIOManager(null);
        SubscriptionManager subs = new SubscriptionManager();

        ServerRouter router = new ServerRouter(
                uiModels,
                subs,
                registry,
                ioManager
        );

        // Register FakeSession with endpoint
        WebSocketEndpoint endpoint = new WebSocketEndpoint();
        endpoint.setServerForTests(new MockMidiServer(registry));
        endpoint.onOpen(session);

        // Incoming message
        String incoming = """
        {
        "type": "get-ui-model",
        "requestId": "req-123",
        "payload": {
            "contextId": "channel.1"
        }
        }
        """;

        router.handleMessage(session, incoming);

        // Wait for async sender thread
        Thread.sleep(50);

        assertFalse(captured.isEmpty(), "Expected a response to be sent");

        JsonObject json = JsonParser.parseString(captured.toString()).getAsJsonObject();
    }
}