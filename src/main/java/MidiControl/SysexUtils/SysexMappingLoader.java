package MidiControl.SysexUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SysexMappingLoader {

    private static final String DEFAULT_RESOURCE = "MidiControl/01V96i_sysex_mappings.json";

    public static List<SysexMapping> loadMappings() {
        try (InputStream is = SysexMappingLoader.class.getClassLoader().getResourceAsStream(DEFAULT_RESOURCE)) {
            InputStreamReader reader = new InputStreamReader(is);
            Type listType = new TypeToken<List<SysexMapping>>() {}.getType();
            return new Gson().fromJson(reader, listType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Sysex mappings", e);
        }
    }

    public static List<SysexMapping> loadMappingsFromString(String json) {
        return new Gson().fromJson(json, new TypeToken<List<SysexMapping>>() {}.getType());
    }

    public static List<SysexMapping> loadMappingsFromResource(String resourceName) {
        try (InputStream is = SysexMappingLoader.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new RuntimeException("Resource not found: " + resourceName);
            }
            InputStreamReader reader = new InputStreamReader(is);
            Type listType = new TypeToken<List<SysexMapping>>() {}.getType();
            return new Gson().fromJson(reader, listType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Sysex mappings from " + resourceName, e);
        }
    }

    public static List<SysexMapping> loadAllMappings() {
        List<String> mappingFiles = Arrays.asList(
            "MidiControl/01V96i_sysex_mappings.json",
            "MidiControl/m7cl_sysex_mappings.json"
            // add more consoles here later
        );

        List<SysexMapping> all = new ArrayList<>();
        for (String file : mappingFiles) {
            all.addAll(loadMappingsFromResource(file));
        }
        return all;
    }
}
