package MidiControl.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import MidiControl.MidiDeviceManager.Configs.DeviceSettings;
import MidiControl.MidiDeviceManager.Configs.SettingsPersistence;
import java.io.File;
import org.junit.jupiter.api.*;

@Tag("unit")
public class SettingsPersistenceTest {

  private static final String INPUT_NAME = "TestInput";
  private static final String OUTPUT_NAME = "TestOutput";

  @BeforeEach
  public void setupTestSettings() {
    System.out.println("Running setupTestSettings()");
    assertTrue(true, "Confirming setupTestSettings() is running");

    DeviceSettings settings = new DeviceSettings();
    settings.inputDeviceIndex = 1;
    settings.inputDeviceName = INPUT_NAME;
    settings.inputDeviceType = "Mock MIDI In";
    settings.outputDeviceIndex = 2;
    settings.outputDeviceName = OUTPUT_NAME;
    settings.outputDeviceType = "Mock MIDI Out";

    SettingsPersistence.save(settings);
  }

  @AfterEach
  public void cleanupSettingsFile() {
    File file = new File(System.getProperty("user.home"), "MidiControl/midi_settings.json");
    if (file.exists()) file.delete();
  }

  @org.junit.jupiter.api.Test
  public void testDeviceSettingsSerialization() {
    DeviceSettings loaded = SettingsPersistence.load();
    assertEquals(1, loaded.inputDeviceIndex);
    assertEquals(INPUT_NAME, loaded.inputDeviceName);
    assertEquals("Mock MIDI In", loaded.inputDeviceType);
    assertEquals(2, loaded.outputDeviceIndex);
    assertEquals(OUTPUT_NAME, loaded.outputDeviceName);
    assertEquals("Mock MIDI Out", loaded.outputDeviceType);
  }
}
