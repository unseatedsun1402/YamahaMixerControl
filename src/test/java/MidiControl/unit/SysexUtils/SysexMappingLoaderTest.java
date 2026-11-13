package MidiControl.unit.SysexUtils;

import static org.junit.jupiter.api.Assertions.*;

import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SysexMappingLoaderTest {
  @Test
  public void testLoadMappingsFromClasspath() {
    List<SysexMapping> mappings =
        SysexMappingLoader.loadMappingsFromResource("MidiControl/m7cl_sysex_mappings.json");

    // Basic sanity checks
    assertNotNull(mappings, "Mappings list should not be null");
    assertFalse(mappings.isEmpty(), "Mappings list should not be empty");

    // Check that a known mapping is present
    SysexMapping first = mappings.get(0);
    assertNotNull(first.getControlGroup(), "First mapping should have a name");
    assertNotNull(first.getControl_id(), "First mapping should have a control_id");

    // Example: verify channel index and comment fields
    System.out.println(
        "Loaded mapping: "
            + first.getControlGroup()
            + " control_id="
            + first.getControl_id()
            + " channel="
            + first.getChannel_index()
            + " comment="
            + first.getComment());
  }

  @Test
  public void testSysexMappingsLoad() {
    List<SysexMapping> mappings = SysexMappingLoader.loadAllMappings();
    List<SysexMapping> v96i =
        SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");
    List<SysexMapping> m7cl =
        SysexMappingLoader.loadMappingsFromResource("MidiControl/m7cl_sysex_mappings.json");
    assertFalse(v96i.isEmpty(), "Expected 01V96i mappings");
    assertFalse(m7cl.isEmpty(), "Expected M7CL mappings");
    assertEquals(v96i.size() + m7cl.size(), mappings.size());
  }

  @Test
  public void testSysexResourceExists() {
    InputStream in =
        SysexMappingLoader.class
            .getClassLoader()
            .getResourceAsStream("MidiControl/m7cl_sysex_mappings.json");

    assertNotNull(in, "Sysex mapping resource should exist on classpath");
  }
}
