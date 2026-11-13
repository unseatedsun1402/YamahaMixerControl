package MidiControl.unit.NrpnUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.sound.midi.ShortMessage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import MidiControl.NrpnUtils.*;

import MidiControl.NrpnUtils.NrpnMapping;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;
import MidiControl.ControlServer.CanonicalInputEvent;
import MidiControl.ControlServer.HardwareInputHandler;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;

public class NrpnMappingLoaderTest {
    @Test
    void v96iAuxAndPanAddressesResolveAndProduceExpectedNrpn() throws Exception {
        List<SysexMapping> sysex = SysexMappingLoader.loadMappingsFromResource(
                "MidiControl/01v96i_sysex_mappings.json");
        CanonicalRegistry registry = new CanonicalRegistry(sysex, new SysexParser(sysex));
        List<NrpnMapping> mappings = NrpnMappingLoader.loadFromResource(
                "MidiControl/nrpn/01v96i_nrpn_mappings.json");
        registry.attachNrpnMappings(mappings);

        int[] auxStarts = {0x007E, 0x00DE, 0x013E, 0x019E,
                0x01FE, 0x025E, 0x02BE, 0x031E};
        for (int aux = 1; aux <= auxStarts.length; aux++) {
            for (int channel = 0; channel < 40; channel++) {
                assertAddress(registry, "kInputAUX.kAUX" + aux + "Level." + channel,
                        auxStarts[aux - 1] + channel);
            }
        }
        for (int channel = 0; channel < 40; channel++) {
            assertAddress(registry, "kInputChannelPan.kChannelPan." + channel, 0x2096 + channel);
        }

        HardwareInputHandler handler = new HardwareInputHandler(new NrpnParser(), new NrpnRegistry());
        for (int[] capture : new int[][] {{22, 0, -63}, {24, 63, 0}, {53, 127, 63}}) {
            int channel = capture[0] - 22;
            ControlInstance pan = registry.resolveCanonicalId("kInputChannelPan.kChannelPan." + channel);
            AtomicInteger received = new AtomicInteger(Integer.MIN_VALUE);
            pan.addListener((instance, value) -> received.set(value));
            CanonicalInputEvent event = null;
            for (int[] cc : new int[][] {{98, capture[0]}, {99, 65}, {6, capture[1]}, {38, capture[1]}}) {
                event = handler.handle(new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, cc[0], cc[1]));
            }
            assertNotNull(event);
            ControlInstance resolved = registry.resolve(event);
            assertEquals(pan, resolved);
            resolved.updateValue(resolved.extractValue(event));
            assertEquals(capture[2], received.get());
        }
    }

    private void assertAddress(CanonicalRegistry registry, String canonicalId, int address) {
        ControlInstance control = registry.resolveCanonicalId(canonicalId);
        int msb = address >> 7;
        int lsb = address & 0x7F;
        ControlInstance resolved = registry.resolveNrpn(msb, lsb);
        assertNotNull(resolved, canonicalId);
        assertEquals(canonicalId, resolved.getCanonicalId());
        List<byte[]> messages = control.buildNrpnChange(0);
        assertArrayEquals(new byte[] {(byte) 0xB0, 99, (byte) msb}, messages.get(0), canonicalId);
        assertArrayEquals(new byte[] {(byte) 0xB0, 98, (byte) lsb}, messages.get(1), canonicalId);
    }

    @Test
    void testLoadAllFromDirectory_LoadsValidJson(@TempDir Path tempDir) throws Exception {
        // Create two small JSON files
        String json1 = "[{\"msb\":\"1\",\"lsb\":\"0\",\"canonical_id\":\"test.a\"}]";
        String json2 = "[{\"msb\":\"2\",\"lsb\":\"0\",\"canonical_id\":\"test.b\"}]";

        Files.writeString(tempDir.resolve("file1.json"), json1);
        Files.writeString(tempDir.resolve("file2.json"), json2);

        List<NrpnMapping> result = NrpnMappingLoader.loadAllFromDirectory(tempDir);

        assertEquals(2, result.size());
        assertEquals(Set.of("test.a", "test.b"),
                result.stream().map(NrpnMapping::getCanonicalId).collect(Collectors.toSet()));
    }

    @Test
    void testLoadAllFromDirectory_ContinuesOnInvalidJson(@TempDir Path tempDir) throws Exception {
        // Valid file
        String validJson = "[{\"msb\":\"1\",\"lsb\":\"0\",\"canonical_id\":\"ok\",\"value_mode\":0}]";
        Files.writeString(tempDir.resolve("good.json"), validJson);

        // Invalid file
        Files.writeString(tempDir.resolve("bad.json"), "{not valid json");

        List<NrpnMapping> result = NrpnMappingLoader.loadAllFromDirectory(tempDir);

        // Should load only the valid one
        assertEquals(1, result.size());
        assertEquals("ok", result.get(0).getCanonicalId());
    }

    @Test
    void testLoadAllFromDirectory_MissingDirectory() {
        Path missing = Path.of("this-directory-does-not-exist-12345");

        List<NrpnMapping> result = NrpnMappingLoader.loadAllFromDirectory(missing);

        assertTrue(result.isEmpty());
    }

    @Test
    void testLoadSingleFile_SkipsInvalidMappings(@TempDir Path tempDir) throws Exception {
        // Create a temporary JSON file with missing required fields
        Path temp = tempDir.resolve("nrpn-invalid.json");

        // Missing canonical_id
        String json = """
            [
                { "msb": "1", "lsb": "0" }
            ]
            """;

        Files.writeString(temp, json);

        // Act
        List<NrpnMapping> result = NrpnMappingLoader.loadAllFromDirectory(temp.getParent());

        // Assert
        assertTrue(result.isEmpty(), 
            "Mappings missing required fields (msb, lsb, canonical_id) must be skipped");
    }
}
