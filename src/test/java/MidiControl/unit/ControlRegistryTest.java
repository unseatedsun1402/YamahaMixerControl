package MidiControl.unit;

import static org.junit.jupiter.api.Assertions.*;

import MidiControl.Controls.ControlRegistry;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ControlRegistryTest {
  @Test
  void testResolveValidMessageByStringJson() {
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

    // Build ControlRegistry from mappings
    ControlRegistry controlRegistry = new ControlRegistry(mappings);

    // Build a message that matches the mapping
    byte[] message =
        new byte[] {
          (byte) 0xF0, (byte) 0x43, (byte) 0x10, (byte) 0x3E,
          (byte) 0x11, (byte) 0x01, (byte) 0x00, (byte) 0x29,
          (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
          (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
          (byte) 0x01, (byte) 0xF7
        };

    // Resolve to Control
    MidiControl.Controls.Control control = controlRegistry.resolveSysex(message);

    assertNotNull(control, "Expected Control to be found");
    assertEquals(41, control.getControlId());
    assertEquals("kInputHA", control.getControlGroup());
  }

  @Test
  void testResolveValidMessageByAllJson() {
    // Load mapping from full JSON
    List<SysexMapping> mappings = SysexMappingLoader.loadAllMappings();

    // Build ControlRegistry from mappings
    ControlRegistry controlRegistry = new ControlRegistry(mappings);

    // Build a message that matches the mapping
    byte[] faderMsg =
        new byte[] {
          (byte) 0xF0, (byte) 0x43, (byte) 0x10, (byte) 0x3E,
          (byte) 0x7F, (byte) 0x01, (byte) 0x1C, (byte) 0x00,
          (byte) 0x1A, (byte) 0x00, (byte) 0x00, (byte) 0x03,
          (byte) 0x14, (byte) 0xF7
        };

    // Resolve to Control
    MidiControl.Controls.Control control = controlRegistry.resolveSysex(faderMsg);

    System.out.println(
        "Resolved Control: "
            + (control != null
                ? control.getControlGroup() + " ID=" + control.getControlId()
                : "null"));

    assertNotNull(control, "Expected Control to be found");
    assertEquals(27, control.getControlId());
    assertEquals("kInputFader", control.getControlGroup());
  }
}
