package MidiControl.SysexUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class SysexMappingLoader {
    private static Logger logger = Logger.getLogger("SysexMappingLoader");

    // private static final String DEFAULT_RESOURCE = "MidiControl/01V96i_sysex_mappings.json";

    // public static List<SysexMapping> loadMappings() {
    //     try (InputStream is =
    //              SysexMappingLoader.class.getClassLoader().getResourceAsStream(DEFAULT_RESOURCE)) {

    //         InputStreamReader reader = new InputStreamReader(is);
    //         Type listType = new TypeToken<List<SysexMapping>>() {}.getType();

    //         List<SysexMapping> mappings = new Gson().fromJson(reader, listType);
    //         initializeMappings(mappings);
    //         return mappings;

    //     } catch (Exception e) {
    //         throw new RuntimeException("Failed to load Sysex mappings", e);
    //     }
    // }

    public static List<SysexMapping> loadMappingsFromString(String json) {
        List<SysexMapping> mappings =
            new Gson().fromJson(json, new TypeToken<List<SysexMapping>>() {}.getType());
        initializeMappings(mappings);
        return mappings;
    }

    public static List<SysexMapping> loadMappingsFromResource(String resourceName) {
        try (InputStream is =
                 SysexMappingLoader.class.getClassLoader().getResourceAsStream(resourceName)) {

            if (is == null) {
                throw new RuntimeException("Resource not found: " + resourceName);
            }

            InputStreamReader reader = new InputStreamReader(is);
            Type listType = new TypeToken<List<SysexMapping>>() {}.getType();

            List<SysexMapping> mappings = new Gson().fromJson(reader, listType);
            initializeMappings(mappings);
            logger.info("Loaded "+mappings.size() + " from "+resourceName);
            return mappings;

        } catch (Exception e) {
            throw new RuntimeException("Failed to load Sysex mappings from " + resourceName, e);
        }
    }

    public static List<SysexMapping> loadAllMappings() {
        List<String> mappingFiles =
            Arrays.asList(
                "MidiControl/01V96i_sysex_mappings.json",
                "MidiControl/m7cl_sysex_mappings.json"
                // add more consoles here later
            );

        List<SysexMapping> all = new ArrayList<>();
        for (String file : mappingFiles) {
            all.addAll(loadMappingsFromResource(file));
        }

        initializeMappings(all);
        return all;
    }

    /** Ensures all mappings compute their derived fields after JSON deserialization. */
    private static void initializeMappings(List<SysexMapping> mappings) {
        if (mappings == null) return;

        for (SysexMapping m : mappings) {
            if (m != null) {
                m.initialize();  // <-- recompute valueByteIndices + indexByteIndices
            }
        }
    }
}