package MidiControl.functional.HardwareToServer;

import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Mocks.MockMidiServer;
import MidiControl.TestUtilities.MidiTestUtils;
import MidiControl.UserInterface.Frontend.GuiBroadcaster;
import MidiControl.UserInterface.CanonicalContextResolver;
import MidiControl.UserInterface.Frontend.GuiBroadcastListener;
import MidiControl.Server.MidiServer;
import MidiControl.Server.SubscriptionManager;
import MidiControl.Routing.WebSocketEndpoint;

import org.junit.jupiter.api.Test;

import jakarta.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.Future;

import javax.sound.midi.SysexMessage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional test:
 *   Inject a SysEx message into the server and verify
 *   that it results in a WebSocket broadcast via AsyncRemote.
 */
public class SendSysexToWebSocketTest {

    /**
     * A simplified FakeSession implementation suitable for functional tests.
     * Captures outbound WebSocket text sent via AsyncRemote.
     */
    static class CapturingSession implements Session {

        private final String id;
        private final StringBuilder sink;
        private boolean open = true;

        CapturingSession(String id, StringBuilder sink) {
            this.id = id;
            this.sink = sink;
        }

        // ---- AsyncRemote implementation ----
        private final RemoteEndpoint.Async asyncRemote = new RemoteEndpoint.Async() {
            @Override
            public Future sendText(String text) {
                sink.append(text);
                return null;
            }

            @Override
            public void sendText(String text, SendHandler handler) {
                sink.append(text);
                handler.onResult(new SendResult()); // Simulate async success
            }

            // Unused operations — throw if called
            @Override public void setSendTimeout(long timeout) {}
            @Override public long getSendTimeout() { return 0; }
            @Override public Future sendBinary(ByteBuffer data) { throw new UnsupportedOperationException();}
            @Override public void sendBinary(ByteBuffer data, SendHandler handler) { throw new UnsupportedOperationException(); }
            @Override public Future sendObject(Object data) { throw new UnsupportedOperationException(); }
            @Override public void sendObject(Object data, SendHandler handler) { throw new UnsupportedOperationException(); }

            @Override
            public void setBatchingAllowed(boolean allowed) throws IOException {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'setBatchingAllowed'");
            }

            @Override
            public boolean getBatchingAllowed() {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'getBatchingAllowed'");
            }

            @Override
            public void flushBatch() throws IOException {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'flushBatch'");
            }

            @Override
            public void sendPing(ByteBuffer applicationData) throws IOException, IllegalArgumentException {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'sendPing'");
            }

            @Override
            public void sendPong(ByteBuffer applicationData) throws IOException, IllegalArgumentException {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'sendPong'");
            }
        };

        @Override public RemoteEndpoint.Async getAsyncRemote() { return asyncRemote; }

        @Override
        public RemoteEndpoint.Basic getBasicRemote() {
            throw new UnsupportedOperationException("BasicRemote must not be used in Async tests.");
        }

        @Override public String getId() { return id; }
        @Override public boolean isOpen() { return open; }
        @Override public void close() { open = false; }
        @Override public void close(CloseReason cr) { open = false; }

        // ---- Stubbed Session methods (unused) ----
        @Override public WebSocketContainer getContainer() { return null; }
        @Override public void addMessageHandler(MessageHandler handler) { throw new UnsupportedOperationException(); }
        @Override public <T> void addMessageHandler(Class<T> clazz, MessageHandler.Whole<T> handler) { throw new UnsupportedOperationException(); }
        @Override public <T> void addMessageHandler(Class<T> clazz, MessageHandler.Partial<T> handler) { throw new UnsupportedOperationException(); }
        @Override public Set<MessageHandler> getMessageHandlers() { return Set.of(); }
        @Override public void removeMessageHandler(MessageHandler handler) { throw new UnsupportedOperationException(); }
        @Override public String getProtocolVersion() { return null; }
        @Override public String getNegotiatedSubprotocol() { return null; }
        @Override public List<Extension> getNegotiatedExtensions() { return List.of(); }
        @Override public boolean isSecure() { return false; }
        @Override public long getMaxIdleTimeout() { return 0; }
        @Override public void setMaxIdleTimeout(long milliseconds) {}
        @Override public void setMaxBinaryMessageBufferSize(int length) {}
        @Override public int getMaxBinaryMessageBufferSize() { return 0; }
        @Override public void setMaxTextMessageBufferSize(int length) {}
        @Override public int getMaxTextMessageBufferSize() { return 0; }
        @Override public URI getRequestURI() { return null; }
        @Override public Map<String, List<String>> getRequestParameterMap() { return Map.of(); }
        @Override public String getQueryString() { return null; }
        @Override public Map<String, String> getPathParameters() { return Map.of(); }
        @Override public Map<String, Object> getUserProperties() { return Map.of(); }
        @Override public Principal getUserPrincipal() { return null; }
        @Override public Set<Session> getOpenSessions() { return Set.of(this); }
    }


    // -------------------------------------------------------------------------
    //                                TEST
    // -------------------------------------------------------------------------

    @Test
    void testSysexBroadcastToWebSocket() throws Exception {

        // ---- Build registry & server ----
        List<SysexMapping> mappings =
                SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");

        SysexParser parser = new SysexParser(mappings);
        CanonicalRegistry registry = new CanonicalRegistry(mappings, parser);

        // Capture WS messages
        StringBuilder captured = new StringBuilder();
        CapturingSession fakeSession = new CapturingSession("S1", captured);

        SubscriptionManager subs = new SubscriptionManager();
        subs.subscribe(fakeSession, "test.context");

        GuiBroadcaster broadcaster = (json, ctx) -> {
            for (Session s : subs.getSubscribers(ctx)) {
                WebSocketEndpoint.send(s, json);
            }
        };

        CanonicalContextResolver resolver = canonicalId -> "test.context";
        GuiBroadcastListener listener = new GuiBroadcastListener(broadcaster, resolver);

        MidiServer server = new MidiServer(registry);
        server.setGuiBroadcastListener(listener);

        // Register FakeSession with endpoint
        WebSocketEndpoint endpoint = new WebSocketEndpoint();
        endpoint.setServerForTests(server);
        endpoint.onOpen(fakeSession);

        // ---- SysEx ----
        byte[] faderMsg = {
                (byte) 0xF0, (byte) 0x43, (byte) 0x10, (byte) 0x3E,
                (byte) 0x7F, (byte) 0x01, (byte) 0x1C, (byte) 0x00,
                (byte) 0x1A, (byte) 0x00, (byte) 0x00, (byte) 0x03,
                (byte) 0x14, (byte) 0xF7
        };

        SysexMessage msg = MidiTestUtils.createSysexMessage(faderMsg);

        server.addtoinputqueue(msg);
        server.processIncomingMidiForTest();

        // ---- WAIT for async sender thread ----
        for (int i = 0; i < 10 && captured.length() == 0; i++) {
            Thread.sleep(10);
        }

        assertFalse(captured.isEmpty(), "Expected WebSocket to receive a broadcast");

        System.out.println("WebSocket received: " + captured);
    }
}