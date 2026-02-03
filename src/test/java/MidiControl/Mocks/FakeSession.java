package MidiControl.Mocks;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import jakarta.websocket.CloseReason;
import jakarta.websocket.Extension;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.SendHandler;
import jakarta.websocket.SendResult;
import jakarta.websocket.Session;

public class FakeSession implements Session {

    private boolean open = true;
    private final String id;

    public String lastSent = null;
    private Consumer<String> remoteSender = null;

    public FakeSession(String id) {
        this.id = id;
    }

    // Inject test message capture
    public void setRemoteSender(Consumer<String> sender) {
        this.remoteSender = sender;
    }

    // -------------------------------------------------------------------------
    // AsyncRemote implementation
    // -------------------------------------------------------------------------
    private final RemoteEndpoint.Async asyncRemote = new RemoteEndpoint.Async() {

        @Override
        public Future<Void> sendText(String text) {
            lastSent = text;
            if (remoteSender != null) remoteSender.accept(text);
            return null;  // fine for tests
        }

        @Override
        public void sendText(String text, SendHandler handler) {
            lastSent = text;
            if (remoteSender != null) remoteSender.accept(text);
            handler.onResult(new SendResult()); // simulate success
        }

        @Override public long getSendTimeout() { return 0; }
        @Override public void setSendTimeout(long timeout) {}

        @Override
        public Future<Void> sendBinary(ByteBuffer data) {
            throw new UnsupportedOperationException("sendBinary not supported in tests");
        }

        @Override
        public void sendBinary(ByteBuffer data, SendHandler handler) {
            throw new UnsupportedOperationException("sendBinary not supported in tests");
        }

        @Override
        public Future<Void> sendObject(Object o) {
            throw new UnsupportedOperationException("sendObject not supported in tests");
        }

        @Override
        public void sendObject(Object o, SendHandler handler) {
            throw new UnsupportedOperationException("sendObject not supported in tests");
        }

        @Override public void setBatchingAllowed(boolean allowed) { throw new UnsupportedOperationException(); }
        @Override public boolean getBatchingAllowed() { throw new UnsupportedOperationException(); }
        @Override public void flushBatch() { throw new UnsupportedOperationException(); }
        @Override public void sendPing(ByteBuffer d) { throw new UnsupportedOperationException(); }
        @Override public void sendPong(ByteBuffer d) { throw new UnsupportedOperationException(); }
    };

    // -------------------------------------------------------------------------
    // Reject BasicRemote to catch old incorrect usage
    // -------------------------------------------------------------------------
    private final RemoteEndpoint.Basic basicRemote = new RemoteEndpoint.Basic() {
        @Override public void sendText(String text) {
            throw new UnsupportedOperationException("BasicRemote.sendText MUST NOT be used");
        }
        @Override public void sendText(String partialMessage, boolean isLast) { throw new UnsupportedOperationException(); }
        @Override public void sendBinary(ByteBuffer data) { throw new UnsupportedOperationException(); }
        @Override public void sendBinary(ByteBuffer data, boolean isLast) { throw new UnsupportedOperationException(); }
        @Override public OutputStream getSendStream() { throw new UnsupportedOperationException(); }
        @Override public Writer getSendWriter() { throw new UnsupportedOperationException(); }
        @Override public void sendObject(Object data) { throw new UnsupportedOperationException(); }
        @Override public void setBatchingAllowed(boolean allowed) { throw new UnsupportedOperationException(); }
        @Override public boolean getBatchingAllowed() { throw new UnsupportedOperationException(); }
        @Override public void flushBatch() { throw new UnsupportedOperationException(); }
        @Override public void sendPing(ByteBuffer applicationData) { throw new UnsupportedOperationException(); }
        @Override public void sendPong(ByteBuffer applicationData) { throw new UnsupportedOperationException(); }
    };

    // -------------------------------------------------------------------------
    // Session implementation
    // -------------------------------------------------------------------------
    @Override public RemoteEndpoint.Async getAsyncRemote() { return asyncRemote; }
    @Override public RemoteEndpoint.Basic getBasicRemote() { throw new UnsupportedOperationException("BasicRemote not allowed"); }

    @Override public String getId() { return id; }
    @Override public boolean isOpen() { return open; }
    @Override public void close() { open = false; }
    @Override public void close(CloseReason cr) { open = false; }

    // ---- Everything else unsupported ----
    @Override public URI getRequestURI() { throw new UnsupportedOperationException(); }
    @Override public Map<String, List<String>> getRequestParameterMap() { throw new UnsupportedOperationException(); }
    @Override public String getQueryString() { throw new UnsupportedOperationException(); }
    @Override public Map<String, String> getPathParameters() { throw new UnsupportedOperationException(); }
    @Override public Map<String, Object> getUserProperties() { throw new UnsupportedOperationException(); }
    @Override public jakarta.websocket.WebSocketContainer getContainer() { throw new UnsupportedOperationException(); }
    @Override public void addMessageHandler(MessageHandler handler) { throw new UnsupportedOperationException(); }
    @Override public <T> void addMessageHandler(Class<T> c, MessageHandler.Whole<T> h) { throw new UnsupportedOperationException(); }
    @Override public <T> void addMessageHandler(Class<T> c, MessageHandler.Partial<T> h) { throw new UnsupportedOperationException(); }
    @Override public Set<MessageHandler> getMessageHandlers() { throw new UnsupportedOperationException(); }
    @Override public void removeMessageHandler(MessageHandler handler) { throw new UnsupportedOperationException(); }
    @Override public String getProtocolVersion() { throw new UnsupportedOperationException(); }
    @Override public String getNegotiatedSubprotocol() { throw new UnsupportedOperationException(); }
    @Override public List<Extension> getNegotiatedExtensions() { throw new UnsupportedOperationException(); }
    @Override public boolean isSecure() { throw new UnsupportedOperationException(); }
    @Override public Principal getUserPrincipal() { throw new UnsupportedOperationException(); }
    @Override public Set<Session> getOpenSessions() { throw new UnsupportedOperationException(); }
    @Override public long getMaxIdleTimeout() { throw new UnsupportedOperationException(); }
    @Override public void setMaxIdleTimeout(long ms) { throw new UnsupportedOperationException(); }
    @Override public void setMaxBinaryMessageBufferSize(int len) { throw new UnsupportedOperationException(); }
    @Override public int getMaxBinaryMessageBufferSize() { throw new UnsupportedOperationException(); }
    @Override public void setMaxTextMessageBufferSize(int len) { throw new UnsupportedOperationException(); }
    @Override public int getMaxTextMessageBufferSize() { throw new UnsupportedOperationException(); }
}