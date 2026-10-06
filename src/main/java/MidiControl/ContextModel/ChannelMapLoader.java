package MidiControl.ContextModel;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Logger;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class ChannelMapLoader {
    private static Logger logger = Logger.getLogger(ChannelMapLoader.class.getName());

    public static List<ChannelSource> loadSources(String deskModel) {
        String resourceName = "MidiControl" + File.separator + "desks" + File.separator + deskModel.toLowerCase() + File.separator + "sources.json";

        try (InputStream is =
                ChannelMapLoader.class.getClassLoader().getResourceAsStream(resourceName)) {

            if (is == null) {
                throw new RuntimeException("Source map not found: " + resourceName);
            }

            InputStreamReader reader = new InputStreamReader(is);
            Type listType = new TypeToken<List<ChannelSource>>() {}.getType();

            logger.info(String.format("Built channel map %s",listType));
            return new Gson().fromJson(reader, listType);

        } catch (Exception e) {
            Logger.getLogger(ChannelMapLoader.class.getName()).severe(String.format("Source Map Not Found %s",resourceName));
            throw new RuntimeException("Failed to load source map: " + resourceName, e);
        }
    }

    public static List<ChannelDestination> loadDestinations(String deskModel) {
        String resourceName = "MidiControl" + File.separator + "desks" + File.separator + deskModel.toLowerCase() + File.separator + "destinations.json";

        try (InputStream is =
                ChannelMapLoader.class.getClassLoader().getResourceAsStream(resourceName)) {

            if (is == null) {
                throw new RuntimeException("Destination map not found: " + resourceName);
            }

            InputStreamReader reader = new InputStreamReader(is);
            Type listType = new TypeToken<List<ChannelDestination>>() {}.getType();

            logger.info(String.format("Built channel map %s",listType));
            return new Gson().fromJson(reader, listType);

        } catch (Exception e) {
            Logger.getLogger(ChannelMapLoader.class.getName()).severe(String.format("Destination Map Not Found %s",resourceName));
            throw new RuntimeException("Failed to load source map: " + resourceName, e);
        }
    }
}
