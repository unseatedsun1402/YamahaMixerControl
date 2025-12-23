package MidiControl.unit.SysexUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

public class SysexParserInitTest {
  @Test
  public void testSysexParserStaticInit() {
    assertDoesNotThrow(
        () -> {
          // Force class initialization
          Class.forName("MidiControl.SysexUtils.SysexParser");
        });
  }
}
