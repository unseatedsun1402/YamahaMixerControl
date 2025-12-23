package MidiControl.unit.NrpnUtils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Tag;

import org.junit.jupiter.api.Test;

import MidiControl.NrpnUtils.NrpnRegistry;
import MidiControl.NrpnUtils.NrpnMapping;

@Tag("unit")
public class NrpnRegistryTest {
    @Test
    public void testLoads01v96MappingsFromClasspath(){
        NrpnRegistry registry = new NrpnRegistry();
        registry.loadFromClasspath("MidiControl/nrpn/01v96i_nrpn_mappings.json");
        List<NrpnMapping> mappings = registry.getMappings();
        NrpnMapping firstMapping = mappings.get(0);
        System.out.println("First mapping: " + firstMapping.getCanonicalId() );
        assertEquals("kInputToAux.kAux1Level.0",firstMapping.getCanonicalId());
    }

    @Test
    public void testLoadFailsOnEmptyStringFromClasspath(){
        NrpnRegistry registry = new NrpnRegistry();
        registry.loadFromClasspath("");
        List<NrpnMapping> mappings = registry.getMappings();
        assertEquals(0,mappings.size());
    }

    @Test
    public void testLoadFailsOnFileNotFoundStringFromClasspath(){
        NrpnRegistry registry = new NrpnRegistry();
        registry.loadFromClasspath("doesnotexist");
        List<NrpnMapping> mappings = registry.getMappings();
        assertEquals(0,mappings.size());
    }

    @Test
    void testMergeAddsMappings() {
        NrpnRegistry registry = new NrpnRegistry();

        // Initial state: empty
        assertEquals(0, registry.getMappings().size());

        // Create minimal synthetic mappings
        NrpnMapping m1 = new NrpnMapping("kGroup.kSub.1", "10", "20");
        NrpnMapping m2 = new NrpnMapping("kGroup.kSub.2", "11", "21");

        registry.merge(List.of(m1, m2));

        List<NrpnMapping> result = registry.getMappings();

        assertEquals(2, result.size());
        assertTrue(result.contains(m1));
        assertTrue(result.contains(m2));
    }

    @Test
    void testClearRemovesAllMappings() {
        NrpnRegistry registry = new NrpnRegistry();

        // Pre-populate registry
        NrpnMapping m1 = new NrpnMapping("kGroup.kSub.1", "10", "20");
        NrpnMapping m2 = new NrpnMapping("kGroup.kSub.2", "11", "21");

        registry.merge(List.of(m1, m2));
        assertEquals(2, registry.getMappings().size());

        // Clear registry
        registry.clear();

        assertTrue(registry.getMappings().isEmpty());
    }
}