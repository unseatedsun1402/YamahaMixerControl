package MidiControl.unit.CanonicalControl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import MidiControl.ControlMappings.CanonicalMapping;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexRegistry;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

public class CanonicalMappingTest {
  @Test
  public void testCanonicalMappingExpansion() {
    // Minimal JSON with one mapping
    String json =
        "[{"
            + "\"control_group\": \"kInputHA\","
            + "\"control_id\": 41,"
            + "\"sub_control\": \"kHAPhantom\","
            + "\"value\": 0,"
            + "\"min_value\": 0,"
            + "\"max_value\": 1,"
            + "\"default_value\": 0,"
            + "\"comment\": \"Off, On\","
            + "\"key\": 266573250601,"
            + "\"parameter_change_format\": ["
            + "240, 67, \"1n\", 62, 17, 1, 0, 41, 0, 0, "
            + "\"cc\", \"cc\", "
            + "\"dd\", \"dd\", \"dd\", \"dd\", \"dd\", "
            + "247"
            + "],"
            + "\"parameter_request_format\": ["
            + "240, 67, \"3n\", 62, 17, 1, 0, 41, 0, 0, "
            + "\"cc\", \"cc\", "
            + "247"
            + "]"
            + "}]";

    // Load mapping from JSON
    List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromString(json);
    SysexRegistry registry = new SysexRegistry(mappings);

    int channelCount = 3; // small test set

    HashMap<String, CanonicalMapping> map = new HashMap<>();

    for (int ch = 1; ch <= channelCount; ch++) {
      String canonicalId =
          mappings.get(0).getControlGroup() + "." + mappings.get(0).getSubControl() + ".ch" + ch;
      CanonicalMapping cm = new CanonicalMapping(canonicalId, mappings.get(0));
      map.put(canonicalId, cm);
    }

    // Assertions
    assertEquals(3, map.size(), "Expected 3 canonical mappings");

    assertTrue(map.containsKey("kInputHA.kHAPhantom.ch1"));
    assertTrue(map.containsKey("kInputHA.kHAPhantom.ch2"));
    assertTrue(map.containsKey("kInputHA.kHAPhantom.ch3"));

    assertDoesNotThrow(() -> map.get("kInputHA.kHAPhantom.ch1"));
    CanonicalMapping cm1 = map.get("kInputHA.kHAPhantom.ch1");
    System.out.println("Canonical Mapping ID: " + cm1.getCanonicalId());
    assertEquals("kInputHA.kHAPhantom.ch1", cm1.getCanonicalId());
    assertEquals(mappings.get(0), cm1.getSysexMapping());
  }

  @Test
  public void updateControlTest() {
    // Minimal JSON with one mapping
    String json =
        "[{"
            + "\"control_group\": \"kInputHA\","
            + "\"control_id\": 41,"
            + "\"sub_control\": \"kHAPhantom\","
            + "\"value\": 0,"
            + "\"min_value\": 0,"
            + "\"max_value\": 1,"
            + "\"default_value\": 0,"
            + "\"comment\": \"Off, On\","
            + "\"key\": 266573250601,"
            + "\"parameter_change_format\": ["
            + "240, 67, \"1n\", 62, 17, 1, 0, 41, 0, 0, "
            + "\"cc\", \"cc\", "
            + "\"dd\", \"dd\", \"dd\", \"dd\", \"dd\", "
            + "247"
            + "],"
            + "\"parameter_request_format\": ["
            + "240, 67, \"3n\", 62, 17, 1, 0, 41, 0, 0, "
            + "\"cc\", \"cc\", "
            + "247"
            + "]"
            + "}]";

    // Load mapping from JSON
    List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromString(json);
    SysexRegistry registry = new SysexRegistry(mappings);

    int channelCount = 1; // small test set

    HashMap<String, CanonicalMapping> map = new HashMap<>();

    for (int ch = 1; ch <= channelCount; ch++) {
      String canonicalId =
          mappings.get(0).getControlGroup() + "." + mappings.get(0).getSubControl() + ".ch" + ch;
      CanonicalMapping cm = new CanonicalMapping(canonicalId, mappings.get(0));
      map.put(canonicalId, cm);
    }

    // Assertions
    assertEquals(1, map.size(), "Expected 1 canonical mappings");

    assertTrue(map.containsKey("kInputHA.kHAPhantom.ch1"));
    assertDoesNotThrow(() -> map.get("kInputHA.kHAPhantom.ch1"));
    CanonicalMapping cm1 = map.get("kInputHA.kHAPhantom.ch1");
    assertDoesNotThrow(() -> cm1.updateValue(127, CanonicalMapping.SOURCE.SYSEX));

    System.out.println("Canonical Mapping ID: " + cm1.getCanonicalId());
    System.out.println("Value: " + cm1.getValue());
    assertEquals(127, cm1.getValue());
    assertEquals(CanonicalMapping.SOURCE.SYSEX, cm1.getLastUpdateSource());
  }
}
