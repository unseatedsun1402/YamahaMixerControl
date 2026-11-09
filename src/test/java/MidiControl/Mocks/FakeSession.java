package MidiControl.Mocks;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import jakarta.websocket.CloseReason;
import jakarta.websocket.Extension;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.MessageHandler.Partial;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.SendHandler;
import jakarta.websocket.SendResult;
import jakarta.websocket.Session;

public class FakeSession implements Session {

    private boolean open = true;
    private final String id;
    private Map<String, List<String>> requestParameters = Map.of();
    private Consumer<String> remoteSender = null;
    private volatile boolean deferAsyncSendCompletion;
    private volatile long asyncSendTimeout;
    private final List<String> sentMessages = java.util.Collections.synchronizedList(new ArrayList<>());
    private final Queue<PendingAsyncSend> pendingAsyncSends = new ConcurrentLinkedQueue<>();
    public String lastSent = null;

    private static final class PendingAsyncSend {
        private final SendHandler handler;

        private PendingAsyncSend(SendHandler handler) {
            this.handler = handler;
        }
    }

    public FakeSession(String id) {
        this.id = id;
    }

    // --- Test helper: inject a sender callback ---
    public void setRemoteSender(Consumer<String> sender) {
        this.remoteSender = sender;
    }

    public void setDeferAsyncSendCompletion(boolean defer) {
        deferAsyncSendCompletion = defer;
    }

    public List<String> getSentMessages() {
        synchronized (sentMessages) {
            return List.copyOf(sentMessages);
        }
    }

    public void completeNextAsyncSend() {
        PendingAsyncSend pending = pendingAsyncSends.poll();
        if (pending == null) {
            throw new IllegalStateException("No asynchronous send is pending");
        }
        pending.handler.onResult(new SendResult());
    }

    private void recordSent(String text) {
        lastSent = text;
        sentMessages.add(text);
        if (remoteSender != null) {
            remoteSender.accept(text);
        }
    }

    public void setRequestParameters(Map<String, List<String>> parameters) {
        this.requestParameters = parameters;
    }

    // --- Minimal BasicRemote stub ---
    private final RemoteEndpoint.Basic basicRemote = new RemoteEndpoint.Basic() {
        @Override
        public void sendText(String text) throws IOException {
            recordSent(text);
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
            throw new UnsupportedOperationException("Unimplemented method 'getSendStream'");
        }

        @Override
        public Writer getSendWriter() throws IOException {
            throw new UnsupportedOperationException("Unimplemented method 'getSendWriter'");
        }
    };

    private final RemoteEndpoint.Async asyncRemote = new RemoteEndpoint.Async() {
        @Override
        public Future<Void> sendText(String text) {
            recordSent(text);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void sendText(String text, SendHandler handler) {
            recordSent(text);
            if (deferAsyncSendCompletion) {
                pendingAsyncSends.add(new PendingAsyncSend(handler));
            } else {
                handler.onResult(new SendResult());
            }
        }

        @Override public Future<Void> sendBinary(java.nio.ByteBuffer data) { throw new UnsupportedOperationException(); }
        @Override public void sendBinary(java.nio.ByteBuffer data, SendHandler handler) { throw new UnsupportedOperationException(); }
        @Override public Future<Void> sendObject(Object data) { throw new UnsupportedOperationException(); }
        @Override public void sendObject(Object data, SendHandler handler) { throw new UnsupportedOperationException(); }
        @Override public long getSendTimeout() { return asyncSendTimeout; }
        @Override public void setSendTimeout(long timeout) { asyncSendTimeout = timeout; }
        @Override public void setBatchingAllowed(boolean allowed) { throw new UnsupportedOperationException(); }
        @Override public boolean getBatchingAllowed() { throw new UnsupportedOperationException(); }
        @Override public void flushBatch() { throw new UnsupportedOperationException(); }
        @Override public void sendPing(java.nio.ByteBuffer applicationData) { throw new UnsupportedOperationException(); }
        @Override public void sendPong(java.nio.ByteBuffer applicationData) { throw new UnsupportedOperationException(); }
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
    @Override public Map<String, List<String>> getRequestParameterMap() { return requestParameters; }
    @Override public String getQueryString() { throw new UnsupportedOperationException(); }
    @Override public Map<String, String> getPathParameters() { throw new UnsupportedOperationException(); }
    @Override public Map<String, Object> getUserProperties() { throw new UnsupportedOperationException(); }
    @Override public void close() throws IOException { open = false; }
    @Override public RemoteEndpoint.Async getAsyncRemote() { return asyncRemote; }
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
        throw new UnsupportedOperationException("Unimplemented method 'addMessageHandler'");
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
    public void close(CloseReason closeReason) throws IOException {
        throw new UnsupportedOperationException("Unimplemented method 'close'");
    }
}