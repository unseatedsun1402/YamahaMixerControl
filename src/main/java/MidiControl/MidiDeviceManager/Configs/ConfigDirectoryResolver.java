package MidiControl.MidiDeviceManager.Configs;

import java.nio.file.*;

public final class ConfigDirectoryResolver {

    private static final String PROPERTY = "midicontrol.config.dir";

    public static Path resolveBaseDirectory() {
        String override = System.getProperty(PROPERTY);

        if (override != null && !override.isBlank()) {
            return Paths.get(override);
        }

        // Default fallback (Windows-friendly)
        return Paths.get(System.getProperty("user.home"), "MidiControlConfig");
    }
}
