package MidiControl.SysexUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;

public class SysexMappingLoader {

    private static final String DEFAULT_RESOURCE = "MidiControl/sysex_mappings.json";

    public static List<SysexMapping> loadMappings() {
        try (InputStream is = SysexMappingLoader.class.getClassLoader().getResourceAsStream(DEFAULT_RESOURCE)) {
            // if (is == null) {
            //     throw new IllegalStateException("Mapping resource not found: " + DEFAULT_RESOURCE);
            // }
            InputStreamReader reader = new InputStreamReader(is);
            Type listType = new TypeToken<List<SysexMapping>>() {}.getType();
            return new Gson().fromJson(reader, listType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load SysEx mappings", e);
        }
    }

    public static List<SysexMapping> loadMappingsFromString(String json) {
        return new Gson().fromJson(json, new TypeToken<List<SysexMapping>>() {}.getType());
    }

    // public static List<SysexMapping> loadMappingsFrom(String resourcePath) {
    //     try (InputStream is = SysexMappingLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
    //         if (is == null) {
    //             throw new IllegalStateException("Mapping resource not found: " + resourcePath);
    //         }
    //         InputStreamReader reader = new InputStreamReader(is);
    //         Type listType = new TypeToken<List<SysexMapping>>() {}.getType();
    //         return new Gson().fromJson(reader, listType);
    //     } catch (Exception e) {
    //         throw new RuntimeException("Failed to load SysEx mappings from " + resourcePath, e);
    //     }
    // }

    // public static List<SysexMapping> loadMappingsFromJson(String json) {
    //     try {
    //         Type listType = new TypeToken<List<SysexMapping>>() {}.getType();
    //         return new Gson().fromJson(json, listType);
    //     } catch (Exception e) {
    //         throw new RuntimeException("Failed to parse SysEx mappings JSON", e);
    //     }
    // }
}
