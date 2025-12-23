package MidiControl.unit.SysexUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;

import MidiControl.SysexUtils.SysexMappingLoader;
import org.junit.jupiter.api.Test;

public class SysexParserNoMappingsTest {
  @Test
  public void testSysexFail() {
    assertThrows(
        RuntimeException.class,
        () -> SysexMappingLoader.loadMappingsFromResource("MidiControl/does_not_exist.json"));
  }
}
