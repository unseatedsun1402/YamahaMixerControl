package MidiControl.unit.NrpnUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import MidiControl.NrpnUtils.*;

import MidiControl.NrpnUtils.NrpnMapping;

public class NrpnMappingLoaderTest {
    @Test
    void testLoadAllFromDirectory_LoadsValidJson() throws Exception {
        Path tempDir = Files.createTempDirectory("nrpn-test");

        // Create two small JSON files
        String json1 = "[{\"msb\":\"1\",\"lsb\":\"0\",\"canonical_id\":\"test.a\"}]";
        String json2 = "[{\"msb\":\"2\",\"lsb\":\"0\",\"canonical_id\":\"test.b\"}]";

        Files.writeString(tempDir.resolve("file1.json"), json1);
        Files.writeString(tempDir.resolve("file2.json"), json2);

        List<NrpnMapping> result = NrpnMappingLoader.loadAllFromDirectory(tempDir);

        assertEquals(2, result.size());
        assertEquals("test.a", result.get(0).getCanonicalId());
        assertEquals("test.b", result.get(1).getCanonicalId());
    }

    @Test
    void testLoadAllFromDirectory_ContinuesOnInvalidJson() throws Exception {
        Path tempDir = Files.createTempDirectory("nrpn-test");

        // Valid file
        String validJson = "[{\"msb\":\"1\",\"lsb\":\"0\",\"canonical_id\":\"ok\"}]";
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
    void testLoadSingleFile_SkipsInvalidMappings() throws Exception {
        // Create a temporary JSON file with missing required fields
        Path temp = Files.createTempFile("nrpn-invalid", ".json");

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
