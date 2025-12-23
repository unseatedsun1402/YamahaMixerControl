package MidiControl.unit.Settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import MidiControl.MidiDeviceManager.Configs.DeviceSettings;
import MidiControl.MidiDeviceManager.Configs.SettingsPersistence;
import java.io.File;
import org.junit.jupiter.api.*;

@Tag("unit")
public class SettingsRestorationTest {

  private static final String TEST_INPUT_NAME = "TestInput";
  private static final String TEST_OUTPUT_NAME = "TestOutput";

  @BeforeEach
  public void setupValidSettingsFile() {
    System.out.println("Running setupValidSettingsFile()");
    DeviceSettings settings = new DeviceSettings();
    settings.inputDeviceIndex = 8;
    settings.inputDeviceName = TEST_INPUT_NAME;
    settings.outputDeviceIndex = 7;
    settings.outputDeviceName = TEST_OUTPUT_NAME;
    settings.inputDeviceType = "External MIDI Port";
    settings.outputDeviceType = "Virtual Synth";
    SettingsPersistence.save(settings);
  }

  @AfterEach
  public void cleanupSettingsFile() {
    System.out.println("@AfterEach: cleanupSettingsFile() executed");
    File file = new File(System.getProperty("user.home"), "MidiControl/midi_settings.json");
    if (file.exists()) file.delete();
  }

  @org.junit.jupiter.api.Test
  public void testLoadValidSettingsJson() {
    DeviceSettings settings = SettingsPersistence.load();
    assertNotNull(settings);
    assertEquals(8, settings.inputDeviceIndex);
    assertEquals(TEST_INPUT_NAME, settings.inputDeviceName);
    assertEquals(7, settings.outputDeviceIndex);
    assertEquals(TEST_OUTPUT_NAME, settings.outputDeviceName);
  }
}
