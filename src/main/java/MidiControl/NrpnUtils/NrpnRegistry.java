package MidiControl.NrpnUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class NrpnRegistry {

    private final List<NrpnMapping> mappings = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(NrpnRegistry.class.getName());
    private static final Gson gson = new Gson();

    public NrpnRegistry() {}

    public List<NrpnMapping> getMappings() {
        return mappings;
    }

    public void loadFromClasspath(String resourcePath) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (input == null) {
                logger.warning("NRPN mapping file not found on classpath: " + resourcePath);
                return;
            }

            NrpnMapping[] loaded = gson.fromJson(new InputStreamReader(input), NrpnMapping[].class);
            replace(List.of(loaded));

            logger.info("Loaded " + loaded.length + " NRPN mappings from classpath.");
        } catch (Exception e) {
            logger.severe("Failed to load NRPN mappings from classpath: " + e.getMessage());
        }
    }

    public static List<NrpnMapping> parseJson(String json) {
        try {
            NrpnMapping[] arr = gson.fromJson(json, NrpnMapping[].class);
            if (arr == null) return List.of();
            return List.of(arr);
        } catch (JsonSyntaxException ex) {
            throw new IllegalArgumentException("Invalid NRPN JSON: " + ex.getMessage(), ex);
        }
    }
    public synchronized void replace(List<NrpnMapping> newMappings) {
        mappings.clear();
        mappings.addAll(newMappings);
        logger.info("NRPN registry replaced with " + newMappings.size() + " mappings.");
    }

    public synchronized void merge(List<NrpnMapping> moreMappings) {
        mappings.addAll(moreMappings);
        logger.info("Merged " + moreMappings.size() + " NRPN mappings (total now " + mappings.size() + ").");
    }

    public synchronized void clear() {
        mappings.clear();
        logger.info("NRPN registry cleared.");
    }
}