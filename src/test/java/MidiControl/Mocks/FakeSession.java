package MidiControl.Mocks;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.websocket.CloseReason;
import jakarta.websocket.Extension;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.MessageHandler.Partial;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;

public class FakeSession implements Session {

    private boolean open = true;
    private final String id;
    private Consumer<String> remoteSender = null;
    public String lastSent = null;

    public FakeSession(String id) {
        this.id = id;
    }

    // --- Test helper: inject a sender callback ---
    public void setRemoteSender(Consumer<String> sender) {
        this.remoteSender = sender;
    }

    // --- Minimal BasicRemote stub ---
    private final RemoteEndpoint.Basic basicRemote = new RemoteEndpoint.Basic() {
        @Override
        public void sendText(String text) throws IOException {
            lastSent = text;
            if (remoteSender != null) {
                remoteSender.accept(text);
            }
        }

        // Everything else can throw UnsupportedOperationException
        @Override public void sendBinary(java.nio.ByteBuffer data) { throw new UnsupportedOperationException(); }
        @Override public void sendText(String partialMessage, boolean isLast) { throw new UnsupportedOperationException(); }
        @Override public void sendBinary(java.nio.ByteBuffer partialByte, boolean isLast) { throw new UnsupportedOperationException(); }
        @Override public void sendObject(Object data) { throw new UnsupportedOperationException(); }
        @Override public void setBatchingAllowed(boolean allowed) { throw new UnsupportedOperationException(); }
        @Override public boolean getBatchingAllowed() { throw new UnsupportedOperationException(); }
        @Override public void flushBatch() { throw new UnsupportedOperationException(); }
        @Override public void sendPing(java.nio.ByteBuffer applicationData) { throw new UnsupportedOperationException(); }
        @Override public void sendPong(java.nio.ByteBuffer applicationData) { throw new UnsupportedOperationException(); }

        @Override
        public OutputStream getSendStream() throws IOException {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getSendStream'");
        }

        @Override
        public Writer getSendWriter() throws IOException {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getSendWriter'");
        }
    };

    @Override
    public RemoteEndpoint.Basic getBasicRemote() {
        return basicRemote;
    }

    // --- Minimal Session implementation ---
    @Override public String getId() { return id; }
    @Override public boolean isOpen() { return open; }

    // Everything else can throw UnsupportedOperationException for now
    @Override public URI getRequestURI() { throw new UnsupportedOperationException(); }
    @Override public Map<String, List<String>> getRequestParameterMap() { throw new UnsupportedOperationException(); }
    @Override public String getQueryString() { throw new UnsupportedOperationException(); }
    @Override public Map<String, String> getPathParameters() { throw new UnsupportedOperationException(); }
    @Override public Map<String, Object> getUserProperties() { throw new UnsupportedOperationException(); }
    @Override public void close() throws IOException { open = false; }
    @Override public RemoteEndpoint.Async getAsyncRemote() { throw new UnsupportedOperationException(); }
    @Override public jakarta.websocket.WebSocketContainer getContainer() { throw new UnsupportedOperationException(); }
    @Override public void addMessageHandler(MessageHandler handler) { throw new UnsupportedOperationException(); }
    @Override public <T> void addMessageHandler(Class<T> clazz, MessageHandler.Whole<T> handler) { throw new UnsupportedOperationException(); }
    @Override public Set<MessageHandler> getMessageHandlers() { throw new UnsupportedOperationException(); }
    @Override public void removeMessageHandler(MessageHandler handler) { throw new UnsupportedOperationException(); }
    @Override public String getProtocolVersion() { throw new UnsupportedOperationException(); }
    @Override public String getNegotiatedSubprotocol() { throw new UnsupportedOperationException(); }
    @Override public List<Extension> getNegotiatedExtensions() { throw new UnsupportedOperationException(); }
    @Override public boolean isSecure() { throw new UnsupportedOperationException(); }
    public boolean isUserInRole(String role) { throw new UnsupportedOperationException(); }
    @Override public Principal getUserPrincipal() { throw new UnsupportedOperationException(); }
    @Override public Set<Session> getOpenSessions() { throw new UnsupportedOperationException(); }

    @Override
    public <T> void addMessageHandler(Class<T> clazz, Partial<T> handler) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addMessageHandler'");
    }

    @Override
    public long getMaxIdleTimeout() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getMaxIdleTimeout'");
    }

    @Override
    public void setMaxIdleTimeout(long milliseconds) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setMaxIdleTimeout'");
    }

    @Override
    public void setMaxBinaryMessageBufferSize(int length) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setMaxBinaryMessageBufferSize'");
    }

    @Override
    public int getMaxBinaryMessageBufferSize() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getMaxBinaryMessageBufferSize'");
    }

    @Override
    public void setMaxTextMessageBufferSize(int length) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setMaxTextMessageBufferSize'");
    }

    @Override
    public int getMaxTextMessageBufferSize() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getMaxTextMessageBufferSize'");
    }

    @Override
    public void close(CloseReason closeReason) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'close'");
    }
}