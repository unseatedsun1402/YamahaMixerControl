package MidiControl.unit.MidiDeviceManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import MidiControl.DeskDiscovery.DeskDiscovery;
import MidiControl.DeskDiscovery.DeskDiscoveryResult;
import MidiControl.MidiDeviceManager.MidiDeviceDTO;
import MidiControl.Mocks.MockMidiIOManager;
import MidiControl.Mocks.MockRehydrationManager;
import MidiControl.Mocks.MockCanonicalRegistry;

public class DeskDiscoveryTest {

    private DeskDiscovery buildDiscovery(
            MockMidiIOManager io,
            MockRehydrationManager rehydration) {

        DeskDiscovery discovery = new DeskDiscovery(io);

        discovery.setRehydrationManager(rehydration);
        discovery.injectNewRegistry(
                new MockCanonicalRegistry());

        return discovery;
    }

    @Test
    public void DiscoverDevicesDoesNotThrowTest() {
        assertDoesNotThrow(() -> new DeskDiscovery(new MockMidiIOManager(null)));
    }

    @Test 
    public void DiscoverDevicesWithNoIOManagerDoesNotThrowTest(){
        assertDoesNotThrow(()->new DeskDiscovery().discoverDeskModel());
    }

    @Test
    public void DiscoverDevicesCanReadMappings() {
        DeskDiscovery discoverer = new DeskDiscovery();

        discoverer.discoverDeskModel();

        assertTrue(
            discoverer.getKnownDeskProfilesSize() > 0);
    }

    @Test
    public void DiscoverDeskModelReturnsNullWhenNoRehydrationManager() {
        DeskDiscovery discoverer = new DeskDiscovery(new MockMidiIOManager(null));

        discoverer.setRehydrationManager(null);

        assertDoesNotThrow(() -> {
            DeskDiscoveryResult model = discoverer.discoverDeskModel();
            assertTrue(model == null);
        });
    }

    @Test
    public void DiscoverDeskModelReturnsNullWhenNoDeskMatches() {

        MockMidiIOManager mockIO =
                new MockMidiIOManager(null);

        MidiDeviceDTO d = new MidiDeviceDTO();
        d.canInput = true;
        d.canOutput = true;
        mockIO.devices.add(d);

        MockRehydrationManager testManager =
                new MockRehydrationManager();

        testManager.respondChannel = -1;

        DeskDiscovery discoverer =
                buildDiscovery(
                        mockIO,
                        testManager);

        DeskDiscoveryResult result =
                discoverer.discoverDeskModel();

        assertNull(result);
    }

    @Test
    public void DiscoverProbesAllMidiChannels() {

        MockMidiIOManager mockIO =
                new MockMidiIOManager(null);

        MidiDeviceDTO d = new MidiDeviceDTO();
        d.canInput = true;
        d.canOutput = true;
        mockIO.devices.add(d);

        MockRehydrationManager mockRehydration =
                new MockRehydrationManager();

        mockRehydration.respondChannel = -1;

        DeskDiscovery discoverer =
                buildDiscovery(
                        mockIO,
                        mockRehydration);

        discoverer.discoverDeskModel();

        int expected =
                discoverer.getKnownDeskProfilesSize() * 16;

        assertEquals(
                expected,
                mockRehydration.probeCallCount);
    }

    @Test
    public void DiscoverMovesToNextDeskWhenAllChannelsTimeout() {

        MockMidiIOManager mockIO =
                new MockMidiIOManager(null);

        MidiDeviceDTO d = new MidiDeviceDTO();
        d.canInput = true;
        d.canOutput = true;
        mockIO.devices.add(d);

        MockRehydrationManager mockRehydration =
                new MockRehydrationManager();

        DeskDiscovery discoverer =
                buildDiscovery(
                        mockIO,
                        mockRehydration);

        discoverer.discoverDeskModel();

        assertEquals(
                discoverer.getKnownDeskProfilesSize() * 16,
                mockRehydration.probeCallCount);
    }

    @Test
    public void DiscoverHandlesSingleChannelResponse() {

        MockMidiIOManager mockIO =
                new MockMidiIOManager(null);

        MidiDeviceDTO d = new MidiDeviceDTO();
        d.canInput = true;
        d.canOutput = true;
        mockIO.devices.add(d);

        MockRehydrationManager mockRehydration =
                new MockRehydrationManager();

        mockRehydration.respondChannel = 7;

        DeskDiscovery discoverer =
                buildDiscovery(
                        mockIO,
                        mockRehydration);

        DeskDiscoveryResult result =
                discoverer.discoverDeskModel();

        assertEquals(
                8,
                mockRehydration.probeCallCount);

        assertEquals(
                "YAMAHA_01V96I",
                result.getModel());
    }
}
