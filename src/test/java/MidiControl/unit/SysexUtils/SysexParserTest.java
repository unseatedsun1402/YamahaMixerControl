package MidiControl.unit.SysexUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;
import MidiControl.SysexUtils.SysexRegistry;
import java.util.List;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

public class SysexParserTest {

  @org.junit.jupiter.api.Test
  public void testprocessMidiMessage_PhantomPowerOn() {
    // Expanded Sysex message based on mapping
    byte[] phantomOn =
        new byte[] {
          (byte) 0xF0, (byte) 0x43, (byte) 0x10, (byte) 0x3E,
          (byte) 0x11, (byte) 0x01, (byte) 0x00, (byte) 0x29,
          (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, // pos 4 -> channel index
          (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
          (byte) 0x01, (byte) 0xF7
        };

         String json =
        "[{"
            + "\"control_group\": \"kInputHA\","
            + "\"control_id\": 41,"
            + "\"sub_control\": \"kHAPhantom\","
            + "\"value\": 0,"
            + "\"min_value\": 0,"
            + "\"max_value\": 1,"
            + "\"max_channels\" : 56,"
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
    
    SysexParser parser = new SysexParser(SysexMappingLoader.loadMappingsFromString(json));
    SysexMapping result = parser.processMidiMessage(phantomOn);
    Logger.getLogger(SysexParserTest.class.getName()).info("Result: " + result);

    // Assert that the parser resolves correctly
    assertEquals("kInputHA",result.getControlGroup());
  }

  @org.junit.jupiter.api.Test
  public void testprocessFailingMidiMessage_InputFader() {
    // Real failing SysEx message from hardware (channel 26 fader value 0x0314)
    byte[] faderMsg =
        new byte[] {
          (byte) 0xF0, (byte) 0x43, (byte) 0x10, (byte) 0x3E,
          (byte) 0x7F, (byte) 0x01, (byte) 0x1C, (byte) 0x00,
          (byte) 0x1A, (byte) 0x00, (byte) 0x00, (byte) 0x03,
          (byte) 0x14, (byte) 0xF7
        };

     String json =
        "[{"
            + "\"control_group\": \"kInputHA\","
            + "\"control_id\": 41,"
            + "\"sub_control\": \"kHAPhantom\","
            + "\"value\": 0,"
            + "\"min_value\": 0,"
            + "\"max_value\": 1,"
            + "\"max_channels\" : 56,"
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

    List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromString(json);
    SysexParser parser = new SysexParser(mappings);

    assertNull(parser.processMidiMessage(faderMsg),"kInputFader is not loaded so registry returns null");
  }

  @Test
  void testResolveValidMessageByStringJsonM7CL() {
    // Minimal JSON with one mapping
    String json =
        "[{"
            + "\"control_group\": \"kInputHA\","
            + "\"control_id\": 41,"
            + "\"sub_control\": \"kHAPhantom\","
            + "\"value\": 0,"
            + "\"min_value\": 0,"
            + "\"max_value\": 1,"
            + "\"max_channels\" : 56,"
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

    // Build a message that matches the mapping
    byte[] message =
        new byte[] {
          (byte) 0xF0, (byte) 0x43, (byte) 0x10, (byte) 0x3E,
          (byte) 0x11, (byte) 0x01, (byte) 0x00, (byte) 0x29,
          (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, // pos 4 -> channel index
          (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
          (byte) 0x01, (byte) 0xF7
        };

    SysexMapping result = registry.resolve(message);

    assertNotNull(result, "Expected mapping to be found");
    assertEquals("kInputHA", result.getControlGroup());
  }

  @Test
  void testResolveNoMatch() {
    SysexRegistry registry = new SysexRegistry(List.of());
    byte[] message = new byte[18];
    SysexMapping result = registry.resolve(message);
    assertNull(result, "Expected no mapping to be found");
  }

  @Test
  void testSlowVsFastPathPerformance() {
      String json =
          "[{" +
              "\"control_group\": \"kInputHA\"," +
              "\"control_id\": 41," +
              "\"sub_control\": \"kHAPhantom\"," +
              "\"value\": 0," +
              "\"min_value\": 0," +
              "\"max_value\": 1," +
              "\"max_channels\" : 56," +
              "\"default_value\": 0," +
              "\"comment\": \"Off, On\"," +
              "\"key\": 266573250601," +
              "\"parameter_change_format\": [" +
                  "240, 67, \"1n\", 62, 17, 1, 0, 41, 0, 0, " +
                  "\"cc\", \"cc\", " +
                  "\"dd\", \"dd\", \"dd\", \"dd\", \"dd\", 247" +
              "]," +
              "\"parameter_request_format\": [" +
                  "240, 67, \"3n\", 62, 17, 1, 0, 41, 0, 0, " +
                  "\"cc\", \"cc\", 247" +
              "]" +
          "}]";

      List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromString(json);
      SysexRegistry registry = new SysexRegistry(mappings);
      SysexParser parser = new SysexParser(mappings);
      int iterations = 200_000;

      byte[] message = new byte[] {
          (byte) 0xF0, (byte) 0x43, (byte) 0x10, (byte) 0x3E,
          (byte) 0x11, (byte) 0x01, (byte) 0x00, (byte) 0x29,
          (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
          (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
          (byte) 0x01, (byte) 0xF7
      };

      Logger log = Logger.getLogger("PerfTest");

      // --- Slow path baseline ---
      long slowStart = System.nanoTime();
      SysexMapping slowResult = null;
      for (int i = 0; i < iterations; i++) {
          slowResult = registry.resolve(message);
      }
      long slowEnd = System.nanoTime();

      assertNotNull(slowResult);
      assertEquals("kInputHA", slowResult.getControlGroup());

      long slowTime = slowEnd - slowStart;
      log.info("Slow path (Java) "+iterations+ " iterations: " + slowTime / 1_000_000 + " ms");

      // --- Fast path (native) ---
      if (!MidiControl.SysexUtils.NativeSysex.isNativeAvailable()) {
          log.warning("Native resolver unavailable — skipping fast path timing");
          return;
      }

      long fastStart = System.nanoTime();
      SysexMapping fastResult = null;
      for (int i = 0; i < 10_000; i++) {
          fastResult = registry.resolveFast(message);
      }
      long fastEnd = System.nanoTime();

      assertNotNull(fastResult);
      assertEquals("kInputHA", fastResult.getControlGroup());

      long fastTime = fastEnd - fastStart;
      log.info("Fast path (native) "+iterations+ " iterations: " + fastTime / 1_000_000 + " ms");

      // --- Sanity check: fast must be faster ---
      assertTrue(fastTime <= slowTime * 1.1,
          "Fast path should not be significantly slower than slow path");
  }
}
