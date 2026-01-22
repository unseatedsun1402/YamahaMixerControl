package MidiControl.TestUtilities;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

public class NativeLoadTest {
  @Test
  public void loadTest(){
    try {
        assertDoesNotThrow(() -> System.load("G:\\WorkingDir\\YamahaMixerControl\\src\\main\\cpp\\MidiControl\\MeterUtils\\native_meter_tools.dll") );
        System.out.println("Loaded OK");
    } catch (UnsatisfiedLinkError e) {
        System.err.println("Load failed: " + e.getMessage());
        e.printStackTrace();
      }
  }
}