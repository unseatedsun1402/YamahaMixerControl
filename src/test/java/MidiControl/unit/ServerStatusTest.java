package MidiControl.unit;

import org.junit.jupiter.api.Test;

import MidiControl.Server.ServerStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerStatusTest {

    @Test
    void includesNetworkAndLocalAddressesWhenDiscoveryIsAvailable() {
        ServerStatus status = ServerStatus.running(
                "192.168.1.20",
                8123,
                "YamahaMixerControl.local",
                null
        );

        assertEquals("Running", status.state());
        assertEquals("http://192.168.1.20:8123/MidiControl/", status.networkAddress());
        assertEquals("http://YamahaMixerControl.local:8123/MidiControl/", status.localAddress());
    }

    @Test
    void explainsWhenLocalDiscoveryIsUnavailable() {
        ServerStatus status = ServerStatus.running(
                "192.168.1.20",
                8123,
                null,
                "Multicast DNS is unavailable"
        );

        assertEquals("Running", status.state());
        assertEquals("http://192.168.1.20:8123/MidiControl/", status.networkAddress());
        assertEquals("Unavailable: Multicast DNS is unavailable", status.localAddress());
    }
}
