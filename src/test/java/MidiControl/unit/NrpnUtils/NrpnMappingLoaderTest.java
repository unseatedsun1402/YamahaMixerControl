package MidiControl.unit.NrpnUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import MidiControl.NrpnUtils.*;

import MidiControl.NrpnUtils.NrpnMapping;

public class NrpnMappingLoaderTest {
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
