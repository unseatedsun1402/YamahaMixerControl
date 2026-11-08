package MidiControl.Routing;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class WebSocketPeerAddressRegistry {
    private static final long ENTRY_LIFETIME_MILLIS = 30_000;
    private static final ConcurrentMap<String, PeerAddress> PEER_ADDRESSES = new ConcurrentHashMap<>();

    private WebSocketPeerAddressRegistry() {}

    public static void register(String connectionId, String remoteAddress) {
        if (connectionId == null || !connectionId.matches("[A-Za-z0-9-]{1,128}")) {
            return;
        }
        long now = System.currentTimeMillis();
        PEER_ADDRESSES.entrySet().removeIf(entry -> entry.getValue().expiresAt() < now);
        PEER_ADDRESSES.put(connectionId, new PeerAddress(remoteAddress, now + ENTRY_LIFETIME_MILLIS));
    }

    public static String consume(String connectionId) {
        if (connectionId == null) {
            return null;
        }
        PeerAddress address = PEER_ADDRESSES.remove(connectionId);
        if (address == null || address.expiresAt() < System.currentTimeMillis()) {
            return null;
        }
        return address.address();
    }

    private record PeerAddress(String address, long expiresAt) {}
}
