package MidiControl.MidiDeviceManager.Configs;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.logging.*;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import MidiControl.MidiServer;
import MidiControl.MidiDeviceManager.MidiDeviceUtils;
import MidiControl.MidiDeviceManager.MidiSettings;

public class Settings{
    private static final String SETTINGS_DIR;
    private static final String SETTINGS_PATH;
    private static MidiSettings cachedSettings;
    private static final Logger logger = Logger.getLogger(Settings.class.getName());

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

        logger.info(
            "Resolved SETTINGS_DIR from " + dirSource + ": " + SETTINGS_DIR);

        ensureSettingsDirectoryExists();
    }

    public static MidiSettings getSettingsOrDefault() {
        if (cachedSettings != null) return cachedSettings;
        File settingsFile = new File(SETTINGS_PATH);
        if (!settingsFile.exists()) {
            logger.warning(
                "Settings file not found at: " + SETTINGS_PATH);
            cachedSettings = new MidiSettings(-1, "Unknown", "Unknown", -1, "Unknown", "Unknown");
            return cachedSettings;
        }
        try (Reader reader = new FileReader(settingsFile)) {
            cachedSettings = new Gson().fromJson(reader, MidiSettings.class);
            return cachedSettings;
        } catch (IOException e) {
            logger.log(Level.WARNING,
                "Failed to read MIDI settings file", e);
            cachedSettings = new MidiSettings(-1, "Unknown", "Unknown", -1, "Unknown", "Unknown");
            return cachedSettings;
        }
    }

    public static void restore(MidiSettings settings) {
        if (settings == null) {
            logger.log(Level.WARNING,
                "Cannot restore devices — settings object is null.");
            return;
        }

        try {
            MidiServer.setInputDevice(settings.inputDeviceIndex);
            logger.info("Restored input device: " + settings.inputDeviceName);
        } catch (MidiUnavailableException e) {
            logger.warning("Failed to restore input device: " + e.getMessage());
        }

        try {
            MidiServer.setOutputDevice(settings.outputDeviceIndex);
            logger.info("Restored output device: " + settings.outputDeviceName);
        } catch (MidiUnavailableException e) {
            logger.warning("Failed to restore output device: " + e.getMessage());
        }
    }

    private static void writeSettings(MidiSettings settings) {
        try {
            ensureSettingsDirectoryExists();
            File settingsFile = new File(SETTINGS_PATH);
            File parentDir = settingsFile.getParentFile();
            if (!parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                logger.info( "Created parent directory: " + parentDir.getAbsolutePath() + " -> " + created);
            }

            try (Writer writer = new FileWriter(settingsFile)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(settings, writer);
                cachedSettings = settings;
                logger.info("Saved MIDI settings to: " + SETTINGS_PATH);
            }
        } catch (IOException e) {
            logger.severe("Failed to write MIDI settings");
        }
    }

    public static boolean ensureSettingsDirectoryExists() {
        File dir = new File(SETTINGS_DIR);
        if (!dir.canWrite()) {
            logger.warning("SETTINGS_DIR is not writable: " + SETTINGS_DIR);
        }
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            logger.info("Ensured SETTINGS_DIR: " + SETTINGS_DIR + " -> " + created);
            return created;
        }
        return true;
    }

    public static MidiSettings getSettings() {
        if (cachedSettings == null) {
            try (Reader reader = new FileReader(SETTINGS_PATH)) {
                cachedSettings = new Gson().fromJson(reader, MidiSettings.class);
            } catch (IOException e) {
                logger.warning("Failed to load MIDI settings");
            }
        }
        return cachedSettings;
    }

    public static String getLastIOAsJson() {
        try (Reader reader = new FileReader(SETTINGS_PATH)) {
            cachedSettings = new Gson().fromJson(reader, MidiSettings.class);
            return new GsonBuilder().setPrettyPrinting().create().toJson(cachedSettings);
        } catch (IOException e) {
            logger.warning("Failed to get json for last IO settings");;
            return "{}";
        }
    }

    public static int getLastInput() {
        if (cachedSettings == null) getLastIOAsJson();
        return cachedSettings != null ? cachedSettings.inputDeviceIndex : -1;
    }

    public static int getLastOutput() {
        if (cachedSettings == null) getLastIOAsJson();
        return cachedSettings != null ? cachedSettings.outputDeviceIndex : -1;
    }

    public static void setLastIO(String json) {
        File settingsFile = new File(SETTINGS_PATH);
        File parentDir = settingsFile.getParentFile();
        if (!parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            logger.info(
                "Created parent directory: " + parentDir.getAbsolutePath() + " -> " + created);
        }

        try (Writer writer = new FileWriter(settingsFile)) {
            writer.write(json);
            logger.info("Saved MIDI settings to: " + SETTINGS_PATH);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to write MIDI settings file", e);
        }
    }


    public static void saveCurrentIO() {
        setLastIO(getCurrentIOAsJson());
    }

    public static void updateInputDevice(int index, String name, String type) {
        logger.info("Invoking updateInput()");
        MidiSettings settings = getSettingsOrDefault();
        settings.inputDeviceIndex = index;
        settings.inputDeviceName = name;
        settings.inputDeviceType = type;
        writeSettings(settings);
    }

    public static void updateOutputDevice(int index, String name, String type) {
        logger.info("Invoking updateOutput()");
        MidiSettings settings = getSettingsOrDefault();
        settings.outputDeviceIndex = index;
        settings.outputDeviceName = name;
        settings.outputDeviceType = type;
        writeSettings(settings);
    }

    public static String getCurrentIOAsJson() {
        MidiDevice.Info inputInfo = MidiServer.midiIn != null ?MidiServer.midiIn.getDeviceInfo(): null;
        MidiDevice.Info outputInfo = MidiServer.midiOut != null ? MidiServer.midiOut.getDeviceInfo() : null;

        int inputIndex = inputInfo != null ? MidiDeviceUtils.getDeviceIndex(inputInfo) : -1;
        int outputIndex = outputInfo != null ? MidiDeviceUtils.getDeviceIndex(outputInfo) : -1;

        MidiSettings current = new MidiSettings(
            inputIndex,
            inputInfo != null ? inputInfo.getName() : "Unknown",
            inputInfo != null ? inputInfo.getDescription() : "Unknown",
            outputIndex,
            outputInfo != null ? outputInfo.getName() : "Unknown",
            outputInfo != null ? outputInfo.getDescription() : "Unknown"
        );
        return new GsonBuilder().setPrettyPrinting().create().toJson(current);
    }
}