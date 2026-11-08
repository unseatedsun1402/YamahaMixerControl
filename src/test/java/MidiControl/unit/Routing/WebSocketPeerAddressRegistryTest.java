package MidiControl.unit.Routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import MidiControl.Routing.WebSocketPeerAddressRegistry;

class WebSocketPeerAddressRegistryTest {
    @Test
    void consumesHandshakePeerAddressOnlyOnce() {
        String connectionId = "test-connection-1";
        WebSocketPeerAddressRegistry.register(connectionId, "192.0.2.10");

        assertEquals("192.0.2.10", WebSocketPeerAddressRegistry.consume(connectionId));
        assertNull(WebSocketPeerAddressRegistry.consume(connectionId));
    }

    @Test
    void ignoresInvalidConnectionIds() {
        WebSocketPeerAddressRegistry.register("invalid id", "192.0.2.10");

        assertNull(WebSocketPeerAddressRegistry.consume("invalid id"));
    }
}
