package MidiControl.unit;

import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SysexMappingLoaderTest {

    @Test
    public void testLoadMappingsFromClasspath() {
        List<SysexMapping> mappings = SysexMappingLoader.loadMappings();

        // Basic sanity checks
        assertNotNull(mappings, "Mappings list should not be null");
        assertFalse(mappings.isEmpty(), "Mappings list should not be empty");

        // Check that a known mapping is present
        SysexMapping first = mappings.get(0);
        assertNotNull(first.getControlGroup(), "First mapping should have a name");
        assertNotNull(first.getControl_id(), "First mapping should have a control_id");

        // Example: verify channel index and comment fields
        System.out.println("Loaded mapping: " + first.getControlGroup() +
                           " control_id=" + first.getControl_id() +
                           " channel=" + first.getChannel_index() +
                           " comment=" + first.getComment());
    }
}