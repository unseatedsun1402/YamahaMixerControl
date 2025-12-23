package MidiControl.MidiDeviceManager.Configs;

import MidiControl.MidiDeviceManager.MidiDeviceUtils;
import MidiControl.MidiDeviceManager.MidiIOManager;
import MidiControl.MidiDeviceManager.MidiSettings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.logging.*;
/**
 * Pure persistence class for reading/writing MIDI IO settings.
 * No device logic. No dependency on MidiServer. No static device state.
 */
public class Settings {

    private static final Logger logger = Logger.getLogger(Settings.class.getName());

    private static final String SETTINGS_DIR;
    private static final String SETTINGS_PATH;

    private static volatile MidiSettings cachedSettings;

    static {
        String dirSource = "MIDI_SETTINGS_DIR (JVM)";
        String dir = System.getProperty("MIDI_SETTINGS_DIR");

        if (dir == null) {
            dir = System.getenv("MIDI_SETTINGS_DIR");
            dirSource = "MIDI_SETTINGS_DIR (env)";
        }

        if (dir == null) {
            String candidate = System.getProperty("user.home") + File.separator + "MidiControl";
            File testDir = new File(candidate);
            if (testDir.exists() && testDir.canWrite()) {
                dir = candidate;
                dirSource = "user.home";
            }
        }

        if (dir == null) {
            dir = System.getProperty("java.io.tmpdir") + File.separator + "MidiControl";
            dirSource = "java.io.tmpdir";
        }

        SETTINGS_DIR = dir.trim();
        SETTINGS_PATH = SETTINGS_DIR + File.separator + "midi_settings.json";

        logger.info("Resolved SETTINGS_DIR from " + dirSource + ": " + SETTINGS_DIR);
        ensureSettingsDirectoryExists();
    }

    // -------------------------------------------------------------------------
    // Loading
    // -------------------------------------------------------------------------

    public static MidiSettings getSettingsOrDefault() {
        if (cachedSettings != null) return cachedSettings;

        File settingsFile = new File(SETTINGS_PATH);
        if (!settingsFile.exists()) {
            logger.warning("Settings file not found at: " + SETTINGS_PATH);
            cachedSettings = MidiSettings.defaultSettings();
            return cachedSettings;
        }

        try (Reader reader = new FileReader(settingsFile)) {
            cachedSettings = new Gson().fromJson(reader, MidiSettings.class);
            return cachedSettings != null ? cachedSettings : MidiSettings.defaultSettings();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to read MIDI settings file", e);
            cachedSettings = MidiSettings.defaultSettings();
            return cachedSettings;
        }
    }

    public static MidiSettings getSettings() {
        if (cachedSettings == null) {
            return getSettingsOrDefault();
        }
        return cachedSettings;
    }

    // -------------------------------------------------------------------------
    // Saving
    // -------------------------------------------------------------------------

    private static void writeSettings(MidiSettings settings) {
        try {
            ensureSettingsDirectoryExists();
            File settingsFile = new File(SETTINGS_PATH);

            try (Writer writer = new FileWriter(settingsFile)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(settings, writer);
                cachedSettings = settings;
                logger.info("Saved MIDI settings to: " + SETTINGS_PATH);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to write MIDI settings", e);
        }
    }

    public static void updateInputDevice(int index, String name, String type) {
        logger.info("Updating input device settings");
        MidiSettings settings = getSettingsOrDefault();
        settings.inputDeviceIndex = index;
        settings.inputDeviceName = name;
        settings.inputDeviceType = type;
        writeSettings(settings);
    }

    public static void updateOutputDevice(int index, String name, String type) {
        logger.info("Updating output device settings");
        MidiSettings settings = getSettingsOrDefault();
        settings.outputDeviceIndex = index;
        settings.outputDeviceName = name;
        settings.outputDeviceType = type;
        writeSettings(settings);
    }

    public static void saveCurrentIO(MidiIOManager io) {
        writeSettings(getCurrentIO(io));
    }

    // -------------------------------------------------------------------------
    // IO Restoration (delegated to IO manager)
    // -------------------------------------------------------------------------

    public static void restore(MidiIOManager io) {
        MidiSettings settings = getSettingsOrDefault();
        if (settings == null) {
            logger.warning("Cannot restore devices — settings object is null.");
            return;
        }

        try {
            io.setInputDevice(settings.inputDeviceIndex);
            logger.info("Restored input device: " + settings.inputDeviceName);
        } catch (Exception e) {
            logger.warning("Failed to restore input device: " + e.getMessage());
        }

        try {
            io.setOutputDevice(settings.outputDeviceIndex);
            logger.info("Restored output device: " + settings.outputDeviceName);
        } catch (Exception e) {
            logger.warning("Failed to restore output device: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // JSON helpers
    // -------------------------------------------------------------------------

    public static MidiSettings getCurrentIO(MidiIOManager io) {
        var in = io.getMidiIn();
        var out = io.getMidiOut();

        int inputIndex = in != null ? MidiDeviceUtils.getDeviceIndex(in.getDeviceInfo()) : -1;
        int outputIndex = out != null ? MidiDeviceUtils.getDeviceIndex(out.getDeviceInfo()) : -1;

        return new MidiSettings(
                inputIndex,
                in != null ? in.getDeviceInfo().getName() : "Unknown",
                in != null ? in.getDeviceInfo().getDescription() : "Unknown",
                outputIndex,
                out != null ? out.getDeviceInfo().getName() : "Unknown",
                out != null ? out.getDeviceInfo().getDescription() : "Unknown"
        );
    }

    public static String getCurrentIOAsJson(MidiIOManager io) {
        return new GsonBuilder().setPrettyPrinting().create().toJson(getCurrentIO(io));
    }

    // -------------------------------------------------------------------------
    // Directory helpers
    // -------------------------------------------------------------------------

    public static boolean ensureSettingsDirectoryExists() {
        File dir = new File(SETTINGS_DIR);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            logger.info("Ensured SETTINGS_DIR: " + SETTINGS_DIR + " -> " + created);
            return created;
        }
        if (!dir.canWrite()) {
            logger.warning("SETTINGS_DIR is not writable: " + SETTINGS_DIR);
        }
        return true;
    }
}
