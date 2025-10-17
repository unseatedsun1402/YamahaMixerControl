package MidiControl.MidiDeviceManager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.logging.*;

import javax.sound.midi.MidiDevice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import MidiControl.MidiServer;

public class Settings{
    private static final String SETTINGS_DIR = System.getProperty("user.home") + File.separator + "MidiControl";
    private static final String SETTINGS_PATH = SETTINGS_DIR + File.separator + "midi_settings.json";
    private static MidiSettings cachedSettings;

    
    private static MidiSettings getSettingsOrDefault() {
        if (cachedSettings == null) {
            try (Reader reader = new FileReader(SETTINGS_PATH)) {
                cachedSettings = new Gson().fromJson(reader, MidiSettings.class);
            } catch (IOException e) {
                Logger.getLogger(MidiSettings.class.getName()).warning("Reading settings failed, defaulting devices to Unknown -1");
                cachedSettings = new MidiSettings(-1, "Unknown", "Unknown", -1, "Unknown", "Unknown");
            }
        }
        return cachedSettings;
    }

    private static void writeSettings(MidiSettings settings) {
        try {
            File dir = new File(SETTINGS_DIR);
            if (!dir.exists()) dir.mkdirs();

            try (Writer writer = new FileWriter(SETTINGS_PATH)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(settings, writer);
                cachedSettings = settings;
                Logger.getLogger(Settings.class.getName()).log(Level.INFO,
                    "Saved MIDI settings to: " + SETTINGS_PATH);
            }
        } catch (IOException e) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE,
                "Failed to write MIDI settings", e);
        }
    }

    public static MidiSettings getSettings() {
        if (cachedSettings == null) {
            try (Reader reader = new FileReader(SETTINGS_PATH)) {
                cachedSettings = new Gson().fromJson(reader, MidiSettings.class);
            } catch (IOException e) {
                Logger.getLogger(Settings.class.getName()).log(Level.WARNING,
                    "Failed to load MIDI settings", e);
            }
        }
        return cachedSettings;
    }

    public static String getLastIOAsJson() {
        try (Reader reader = new FileReader(SETTINGS_PATH)) {
            cachedSettings = new Gson().fromJson(reader, MidiSettings.class);
            return new GsonBuilder().setPrettyPrinting().create().toJson(cachedSettings);
        } catch (IOException e) {
            Logger.getLogger(Settings.class.getName()).log(Level.WARNING,
                "Failed to read MIDI settings file", e);
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
        String userHome = System.getProperty("user.home");
        File settingsDir = new File(userHome + File.separator + "MidiControl");

        if (!settingsDir.exists()) {
            settingsDir.mkdirs();
        }

        File settingsFile = new File(settingsDir, "midi_settings.json");

        try (Writer writer = new FileWriter(settingsFile)) {
            writer.write(json);
            Logger.getLogger(Settings.class.getName()).log(Level.INFO,
                "Saved MIDI settings to: " + settingsFile.getAbsolutePath());
        } catch (IOException e) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE,
                "Failed to write MIDI settings file", e);
        }
    }

    public static void saveCurrentIO() {
        setLastIO(getCurrentIOAsJson());
    }

    public static void updateInputDevice(int index, String name, String type) {
        MidiSettings settings = getSettingsOrDefault();
        settings.inputDeviceIndex = index;
        settings.inputDeviceName = name;
        settings.inputDeviceType = type;
        writeSettings(settings);
    }

    public static void updateOutputDevice(int index, String name, String type) {
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