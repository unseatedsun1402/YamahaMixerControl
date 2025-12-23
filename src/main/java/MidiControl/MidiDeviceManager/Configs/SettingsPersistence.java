package MidiControl.MidiDeviceManager.Configs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SettingsPersistence {
  private static final String SETTINGS_FILENAME = "midi_settings.json";
  private static final String SUBDIR = "MidiControl";

  private static File resolveSettingsFile() {
    String basePath = System.getProperty("user.home");
    File settingsDir = new File(basePath, SUBDIR);

    if (!settingsDir.exists()) {
      settingsDir.mkdirs();
    }

    if (!settingsDir.canWrite()) {
      Logger.getLogger(SettingsPersistence.class.getName())
          .warning("Settings dir not writable: " + settingsDir);
      settingsDir = new File(System.getProperty("java.io.tmpdir"), SUBDIR);
      settingsDir.mkdirs();
    }

    File settingsFile = new File(settingsDir, SETTINGS_FILENAME);
    Logger.getLogger(SettingsPersistence.class.getName())
        .info("Resolved SETTINGS_PATH: " + settingsFile.getAbsolutePath());
    return settingsFile;
  }

  public static DeviceSettings load() {
    File file = resolveSettingsFile();
    Logger.getLogger(SettingsPersistence.class.getName()).info("Settings resolved from filepath");
    try (FileReader reader = new FileReader(file)) {
      Logger.getLogger(SettingsPersistence.class.getName()).info("Reading file into json");
      Gson gson = new Gson();
      return gson.fromJson(reader, DeviceSettings.class);
    } catch (IOException e) {
      Logger.getLogger(SettingsPersistence.class.getName())
          .log(Level.WARNING, "Failed to load settings", e);
      return new DeviceSettings(); // return defaults
    }
  }

  public static void save(DeviceSettings settings) {
    File file = resolveSettingsFile();
    try (Writer writer = new FileWriter(file)) {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      gson.toJson(settings, writer);
      Logger.getLogger(SettingsPersistence.class.getName())
          .info("Saving settings: " + new Gson().toJson(settings));
    } catch (IOException e) {
      Logger.getLogger(SettingsPersistence.class.getName())
          .log(Level.WARNING, "Failed to save settings", e);
    }
  }
}
