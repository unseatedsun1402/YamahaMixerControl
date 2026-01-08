package MidiControl.functional.HardwareToServer;

import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.TestUtilities.MidiTestUtils;
import MidiControl.UserInterface.Frontend.GuiBroadcaster;
import MidiControl.UserInterface.CanonicalContextResolver;
import MidiControl.UserInterface.Frontend.GuiBroadcastListener;
import MidiControl.Server.MidiServer;
import MidiControl.Server.SubscriptionManager;
import MidiControl.Routing.WebSocketEndpoint;

import org.junit.jupiter.api.Test;

import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Extension;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.MessageHandler.Partial;
import jakarta.websocket.MessageHandler.Whole;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.RemoteEndpoint.Async;

import javax.sound.midi.SysexMessage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class SendSysexToWebSocketTest {

    // ---------------------------------------------------------
    // Fake WebSocket Session that captures outgoing text
    // ---------------------------------------------------------
    static class CapturingSession implements Session {

        private final String id;
        private final StringBuilder sink;

        CapturingSession(String id, StringBuilder sink) {
            this.id = id;
            this.sink = sink;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public RemoteEndpoint.Basic getBasicRemote() {
            return new RemoteEndpoint.Basic() {
                @Override
                public void sendText(String text) throws IOException {
                    sink.append(text);
                }

                @Override
                public void setBatchingAllowed(boolean allowed) throws IOException {
                    throw new UnsupportedOperationException("Unimplemented method 'setBatchingAllowed'");
                }

                @Override
                public boolean getBatchingAllowed() {
                    throw new UnsupportedOperationException("Unimplemented method 'getBatchingAllowed'");
                }
                @Override
                public void flushBatch() throws IOException {throw new UnsupportedOperationException("Unimplemented method 'flushBatch'");}
                @Override
                public void sendPing(ByteBuffer applicationData) throws IOException, IllegalArgumentException {
                    throw new UnsupportedOperationException("Unimplemented method 'sendPing'");
                }
                @Override
                public void sendPong(ByteBuffer applicationData) throws IOException, IllegalArgumentException {
                    throw new UnsupportedOperationException("Unimplemented method 'sendPong'");
                }
                @Override
                public void sendBinary(ByteBuffer data) throws IOException {throw new UnsupportedOperationException("Unimplemented method 'sendBinary'");
                }
                @Override
                public void sendText(String partialMessage, boolean isLast) throws IOException {
                    throw new UnsupportedOperationException("Unimplemented method 'sendText'");
                }
                @Override
                public void sendBinary(ByteBuffer partialByte, boolean isLast) throws IOException {
                    throw new UnsupportedOperationException("Unimplemented method 'sendBinary'");
                }
                @Override
                public OutputStream getSendStream() throws IOException {
                    throw new UnsupportedOperationException("Unimplemented method 'getSendStream'");
                }
                @Override
                public Writer getSendWriter() throws IOException {throw new UnsupportedOperationException("Unimplemented method 'getSendWriter'");
                }
                @Override
                public void sendObject(Object data) throws IOException, EncodeException {
                    throw new UnsupportedOperationException("Unimplemented method 'sendObject'");}
            };
        }

        @Override public boolean isOpen() { return true; }

        @Override
        public WebSocketContainer getContainer() {
            throw new UnsupportedOperationException("Unimplemented method 'getContainer'");
        }
        @Override
        public void addMessageHandler(MessageHandler handler) throws IllegalStateException {
            throw new UnsupportedOperationException("Unimplemented method 'addMessageHandler'");
        }
        @Override
        public <T> void addMessageHandler(Class<T> clazz, Whole<T> handler) {
            throw new UnsupportedOperationException("Unimplemented method 'addMessageHandler'");
        }
        @Override
        public <T> void addMessageHandler(Class<T> clazz, Partial<T> handler) {
            throw new UnsupportedOperationException("Unimplemented method 'addMessageHandler'");
        }
        @Override
        public Set<MessageHandler> getMessageHandlers() {
            throw new UnsupportedOperationException("Unimplemented method 'getMessageHandlers'");
        }
        @Override
        public void removeMessageHandler(MessageHandler handler) {
            throw new UnsupportedOperationException("Unimplemented method 'removeMessageHandler'");
        }
        @Override
        public String getProtocolVersion() {
            throw new UnsupportedOperationException("Unimplemented method 'getProtocolVersion'");
        }
        @Override
        public String getNegotiatedSubprotocol() {
            throw new UnsupportedOperationException("Unimplemented method 'getNegotiatedSubprotocol'");
        }
        @Override
        public List<Extension> getNegotiatedExtensions() {
            throw new UnsupportedOperationException("Unimplemented method 'getNegotiatedExtensions'");
        }
        @Override
        public boolean isSecure() {
            throw new UnsupportedOperationException("Unimplemented method 'isSecure'");
        }
        @Override
        public long getMaxIdleTimeout() {
            throw new UnsupportedOperationException("Unimplemented method 'getMaxIdleTimeout'");
        }
        @Override
        public void setMaxIdleTimeout(long milliseconds) {
            throw new UnsupportedOperationException("Unimplemented method 'setMaxIdleTimeout'");
        }
        @Override
        public void setMaxBinaryMessageBufferSize(int length) {
            throw new UnsupportedOperationException("Unimplemented method 'setMaxBinaryMessageBufferSize'");
        }
        @Override
        public int getMaxBinaryMessageBufferSize() {
            throw new UnsupportedOperationException("Unimplemented method 'getMaxBinaryMessageBufferSize'");
        }
        @Override
        public void setMaxTextMessageBufferSize(int length) {
            throw new UnsupportedOperationException("Unimplemented method 'setMaxTextMessageBufferSize'");
        }
        @Override
        public int getMaxTextMessageBufferSize() {
            throw new UnsupportedOperationException("Unimplemented method 'getMaxTextMessageBufferSize'");
        }
        @Override
        public Async getAsyncRemote() {
            throw new UnsupportedOperationException("Unimplemented method 'getAsyncRemote'");
        }
        @Override
        public void close() throws IOException {
            throw new UnsupportedOperationException("Unimplemented method 'close'");
        }
        @Override
        public void close(CloseReason closeReason) throws IOException {
            throw new UnsupportedOperationException("Unimplemented method 'close'");
        }
        @Override
        public URI getRequestURI() {
            throw new UnsupportedOperationException("Unimplemented method 'getRequestURI'");
        }
        @Override
        public Map<String, List<String>> getRequestParameterMap() {
            throw new UnsupportedOperationException("Unimplemented method 'getRequestParameterMap'");
        }
        @Override
        public String getQueryString() {
            throw new UnsupportedOperationException("Unimplemented method 'getQueryString'");
        }
        @Override
        public Map<String, String> getPathParameters() {
            throw new UnsupportedOperationException("Unimplemented method 'getPathParameters'");
        }
        @Override
        public Map<String, Object> getUserProperties() {
            throw new UnsupportedOperationException("Unimplemented method 'getUserProperties'");
        }
        @Override
        public Principal getUserPrincipal() {throw new UnsupportedOperationException("Unimplemented method 'getUserPrincipal'");}
        @Override
        public Set<Session> getOpenSessions() {throw new UnsupportedOperationException("Unimplemented method 'getOpenSessions'"); }

        // All other Session methods can be left unimplemented or return null
    }

    @Test
    void testSysexBroadcastToWebSocket() throws Exception {

        // ---------------------------------------------------------
        // Capture WebSocket output
        // ---------------------------------------------------------
        StringBuilder captured = new StringBuilder();
        Session fakeSession = new CapturingSession("S1", captured);

        // ---------------------------------------------------------
        // Build real server components
        // ---------------------------------------------------------
        List<SysexMapping> mappings = SysexMappingLoader.loadAllMappings();
        SysexParser parser = new SysexParser(mappings);
        CanonicalRegistry registry = new CanonicalRegistry(mappings, parser);

        SubscriptionManager subs = new SubscriptionManager();
        subs.subscribe(fakeSession, "test.context");

        // Real broadcaster → uses WebSocketEndpoint.send()
        GuiBroadcaster broadcaster = (json, ctx) -> {
            for (Session s : subs.getSubscribers(ctx)) {
                WebSocketEndpoint.send(s, json);
            }
        };

        CanonicalContextResolver resolver = canonicalId -> "test.context";

        GuiBroadcastListener listener = new GuiBroadcastListener(broadcaster, resolver);

        MidiServer server = new MidiServer(registry);
        server.setGuiBroadcastListener(listener);

        // ---------------------------------------------------------
        // Feed a real SYSEX message
        // ---------------------------------------------------------
        byte[] faderMsg = {
            (byte) 0xF0, (byte) 0x43, (byte) 0x10, (byte) 0x3E,
            (byte) 0x7F, (byte) 0x01, (byte) 0x1C, (byte) 0x00,
            (byte) 0x1A, (byte) 0x00, (byte) 0x00, (byte) 0x03,
            (byte) 0x14, (byte) 0xF7
        };

        SysexMessage msg = MidiTestUtils.createSysexMessage(faderMsg);

        server.addtoinputqueue(msg);
        server.processIncomingMidiForTest();

        // ---------------------------------------------------------
        // Assertions
        // ---------------------------------------------------------
        assertFalse(captured.toString().isEmpty(),
            "Expected WebSocket to receive a broadcast");

        System.out.println("WebSocket received: " + captured);
    }
}