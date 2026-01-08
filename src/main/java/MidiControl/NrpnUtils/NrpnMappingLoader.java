package MidiControl.NrpnUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class NrpnMappingLoader {

    private static final Logger logger = Logger.getLogger(NrpnMappingLoader.class.getName());

    private static final Type LIST_TYPE =
            new TypeToken<List<NrpnMapping>>() {}.getType();

    public static List<NrpnMapping> loadFromResource(String resourceName) {
        try (InputStream is =
                     NrpnMappingLoader.class.getClassLoader().getResourceAsStream(resourceName)) {

            if (is == null) {
                throw new RuntimeException("NRPN resource not found: " + resourceName);
            }

            InputStreamReader reader = new InputStreamReader(is);
            List<NrpnMapping> parsed = new Gson().fromJson(reader, LIST_TYPE);

            return validateList(parsed, resourceName);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load NRPN mappings from " + resourceName, e);
        }
    }

    public static List<NrpnMapping> loadFromString(String json) {
        List<NrpnMapping> parsed = new Gson().fromJson(json, LIST_TYPE);
        return validateList(parsed, "<string>");
    }

    public static List<NrpnMapping> loadAllFromDirectory(Path dir) {
        List<NrpnMapping> all = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
            for (Path file : stream) {
                try {
                    List<NrpnMapping> mappings = loadSingleFile(file);
                    all.addAll(mappings);
                    logger.info("Loaded NRPN file: " + file.getFileName());
                } catch (Exception ex) {
                    logger.warning("Failed to load NRPN file: " + file + " → " + ex.getMessage());
                }
            }
        } catch (Exception ex) {
            logger.warning("Failed to scan NRPN directory: " + dir + " → " + ex.getMessage());
        }

        return all;
    }

    public static List<NrpnMapping> loadAllFromResources(List<String> resourceNames) {
        List<NrpnMapping> all = new ArrayList<>();

        for (String res : resourceNames) {
            all.addAll(loadFromResource(res));
        }

        return all;
    }

    private static List<NrpnMapping> loadSingleFile(Path file) throws Exception {
        String json = Files.readString(file);
        List<NrpnMapping> parsed = new Gson().fromJson(json, LIST_TYPE);
        return validateList(parsed, file.toString());
    }

    private static List<NrpnMapping> validateList(List<NrpnMapping> parsed, String source) {
        List<NrpnMapping> valid = new ArrayList<>();

        for (NrpnMapping m : parsed) {
            if (isValid(m)) {
                valid.add(m);
            } else {
                logger.warning(
                    "Invalid NRPN mapping in " + source +
                    ": missing required fields (msb, lsb, canonicalId) → " + m
                );
            }
        }

        return valid;
    }

    private static boolean isValid(NrpnMapping m) {
        return m.getMsb() != null &&
               m.getLsb() != null &&
               m.getCanonicalId() != null &&
               !m.getCanonicalId().isBlank();
    }
}