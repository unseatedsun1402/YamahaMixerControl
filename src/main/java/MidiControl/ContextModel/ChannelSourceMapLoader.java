package MidiControl.ContextModel;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Logger;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class ChannelSourceMapLoader {

    public static List<ChannelSource> load(String deskModel) {
        String resourceName = "MidiControl" + File.separator + "desks" + File.separator + deskModel.toLowerCase() + File.separator + "sources.json";

        try (InputStream is =
                ChannelSourceMapLoader.class.getClassLoader().getResourceAsStream(resourceName)) {

            if (is == null) {
                throw new RuntimeException("Source map not found: " + resourceName);
            }

            InputStreamReader reader = new InputStreamReader(is);
            Type listType = new TypeToken<List<ChannelSource>>() {}.getType();

            return new Gson().fromJson(reader, listType);

        } catch (Exception e) {
            Logger.getLogger(ChannelSourceMapLoader.class.getName()).severe(String.format("Source Map Not Found %s",resourceName));
            throw new RuntimeException("Failed to load source map: " + resourceName, e);
        }
    }
}
