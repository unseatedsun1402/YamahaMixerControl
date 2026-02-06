package MidiControl.SystemTools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Logger;


public final class NativeLoader {
    private static Logger logger = Logger.getLogger(NativeLoader.class.getName());

    public static void load() {
        loadLibrary("native_meter_tools");
        loadLibrary("native_sysex");
    }

    public static boolean loadLibrary(String baseName) {
        String os = getOS();
        String mappedName = mapLibraryName(os, baseName);

        String resourcePath = "/MidiControl/native/" + os + "/" + mappedName;
        if(! new File(resourcePath).exists()) return false;
        try (InputStream in = NativeLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new RuntimeException("Native library not found in resources: " + resourcePath);
            }

            // Create unique temp file for Tomcat safe‑reloads
            File temp = File.createTempFile(baseName + "-", getExtension(os));
            temp.deleteOnExit();

            Files.copy(in, temp.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            System.load(temp.getAbsolutePath());
            return true;
        } catch (IOException e) {
            logger.severe("Failed loading native library: " + resourcePath + " " + e);
            return false;
        }
    }

    private static String getOS() {
        String name = System.getProperty("os.name").toLowerCase();

        if (name.contains("win")) return "windows";
        if (name.contains("mac") || name.contains("osx")) return "osx";
        return "linux";
    }

    private static String mapLibraryName(String os, String base) {
        return switch (os) {
            case "windows" -> base + ".dll";
            case "osx"     -> "lib" + base + ".dylib";
            default        -> "lib" + base + ".so";
        };
    }

    private static String getExtension(String os) {
        return switch (os) {
            case "windows" -> ".dll";
            case "osx"     -> ".dylib";
            default        -> ".so";
        };
    }
}


