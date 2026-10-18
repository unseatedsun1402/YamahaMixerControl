package MidiControl.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EmbeddedPatchEndpointsTest {

    @LocalServerPort
    private int port;

    @Test
    void patchBanksBuildUiModelsAndExposeBothMaps() throws Exception {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpResponse<String> settings = httpClient.send(
                HttpRequest.newBuilder(uri("/MidiControl/settings.html")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, settings.statusCode());

        try (SocketMessages socket = new SocketMessages(
                URI.create("ws://localhost:" + port + "/MidiControl/endpoint"))) {
            JsonObject inputBank = socket.requestAndAwait("""
                    {"type":"get-ui-bank","payload":{"bankId":"bank.inputs"}}
                    """, "ui-bank");
            String inputContext = firstContext(inputBank);

            JsonObject inputModel = socket.requestAndAwait(uiModelRequest(inputContext), "ui-model");
            assertFalse(inputModel.getAsJsonArray("controls").isEmpty(),
                    "Input patch view should contain a selector control");
            assertMap(httpClient, "/MidiControl/source-map");

            JsonObject outputBank = socket.requestAndAwait("""
                    {"type":"get-ui-bank","payload":{"bankId":"bank.outputs"}}
                    """, "ui-bank");
            String outputContext = firstContext(outputBank);

            JsonObject outputModel = socket.requestAndAwait(uiModelRequest(outputContext), "ui-model");
            assertFalse(outputModel.getAsJsonArray("controls").isEmpty(),
                    "Output patch view should contain a selector control");
            assertMap(httpClient, "/MidiControl/dest-map");
        }
    }

    private String firstContext(JsonObject bank) {
        JsonArray contexts = bank.getAsJsonArray("contexts");
        assertTrue(contexts.size() > 0, "Patch bank should contain at least one context");
        return contexts.get(0).getAsString();
    }

    private String uiModelRequest(String contextId) {
        return """
                {"type":"get-ui-model","requestId":"patch-model","payload":{"contextId":"%s","uiType":"basic-patch-view"}}
                """.formatted(contextId);
    }

    private void assertMap(HttpClient client, String path) throws Exception {
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(uri(path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(JsonParser.parseString(response.body()).isJsonArray(),
                path + " should return a JSON array");
        assertFalse(JsonParser.parseString(response.body()).getAsJsonArray().isEmpty(),
                path + " should return map entries");
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private static final class SocketMessages implements AutoCloseable {
        private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        private final AtomicReference<Throwable> failure = new AtomicReference<>();
        private final StringBuilder fragments = new StringBuilder();
        private final WebSocket socket;

        SocketMessages(URI uri) {
            socket = HttpClient.newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(uri, new WebSocket.Listener() {
                        @Override
                        public void onOpen(WebSocket webSocket) {
                            webSocket.request(1);
                        }

                        @Override
                        public synchronized java.util.concurrent.CompletionStage<?> onText(
                                WebSocket webSocket, CharSequence data, boolean last) {
                            fragments.append(data);
                            if (last) {
                                messages.add(fragments.toString());
                                fragments.setLength(0);
                            }
                            webSocket.request(1);
                            return null;
                        }

                        @Override
                        public void onError(WebSocket webSocket, Throwable error) {
                            failure.set(error);
                        }
                    })
                    .join();
        }

        JsonObject requestAndAwait(String request, String expectedType) throws Exception {
            socket.sendText(request, true).join();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (System.nanoTime() < deadline) {
                Throwable error = failure.get();
                if (error != null) {
                    throw new AssertionError("WebSocket failed", error);
                }

                String message = messages.poll(100, TimeUnit.MILLISECONDS);
                if (message == null) {
                    continue;
                }
                JsonObject parsed = JsonParser.parseString(message).getAsJsonObject();
                if (expectedType.equals(parsed.get("type").getAsString())) {
                    return parsed.getAsJsonObject("payload");
                }
            }
            throw new AssertionError("Timed out waiting for WebSocket message type " + expectedType);
        }

        @Override
        public void close() {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "test complete").join();
        }
    }
}
