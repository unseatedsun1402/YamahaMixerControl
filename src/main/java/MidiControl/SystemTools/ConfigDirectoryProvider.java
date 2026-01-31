package MidiControl.SystemTools;
    
import java.io.File;

public final class ConfigDirectoryProvider {

    private ConfigDirectoryProvider() {}

    /**
     * Returns a safe, unprivileged per-user config directory based on OS.
     * Never requires elevation or admin rights.
     */
    public static File getConfigDirectory(String appName) {
        String os = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");

        File dir;

        if (os.contains("win")) {
            // Windows: %APPDATA%\AppName
            String appData = System.getenv("APPDATA");
            if (appData == null) {
                // Extremely rare fallback, but safe
                appData = home + File.separator + "AppData" +
                             File.separator + "Roaming";
            }
            dir = new File(appData, appName);

        } else if (os.contains("mac")) {
            // macOS: ~/Library/Application Support/AppName
            dir = new File(home + "/Library/Application Support/" + appName);

        } else {
            // Linux / Unix / BSD: ~/.config/AppName
            String xdg = System.getenv("XDG_CONFIG_HOME");
            if (xdg == null || xdg.isBlank()) {
                xdg = home + "/.config";
            }
            dir = new File(xdg, appName);
        }

        // Create directory if needed
        if (!dir.exists()) {
            dir.mkdirs(); // safe, unprivileged
        }

        return dir;
    }

    /**
     * Convenience method: returns full path as String.
     */
    public static String getConfigPath(String appName) {
        return getConfigDirectory(appName).getAbsolutePath();
    }
}
