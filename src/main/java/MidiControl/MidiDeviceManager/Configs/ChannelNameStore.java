package MidiControl.MidiDeviceManager.Configs;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.*;
import com.google.gson.stream.JsonWriter;

public class ChannelNameStore {

    private static final Object LOCK = new Object();
    private static List<JsonObject> channels;
    private static File file;

    public static void initialize(File jsonFile) throws IOException {
        synchronized (LOCK) {
            file = jsonFile;

            try (FileReader reader = new FileReader(file)) {
                JsonArray arr = JsonParser.parseReader(reader).getAsJsonArray();
                channels = new ArrayList<>();
                for (JsonElement el : arr) {
                    channels.add(el.getAsJsonObject());
                }
            }
        }
    }

    public static List<JsonObject> getAll() {
        synchronized (LOCK) {
            return Collections.unmodifiableList(channels);
        }
    }

    public static void setName(int channel, String name) throws IOException {
        synchronized (LOCK) {
            if (channel < 1 || channel > channels.size()) {
                throw new IllegalArgumentException("Channel out of range");
            }

            JsonObject obj = channels.get(channel - 1);
            obj.addProperty("short", name);

            writeToDisk();
        }
    }

    private static void writeToDisk() throws IOException {
        File tmp = new File(file.getAbsolutePath() + ".tmp");

        try (FileWriter fw = new FileWriter(tmp, StandardCharsets.UTF_8);
            JsonWriter jw = new GsonBuilder().setPrettyPrinting().create().newJsonWriter(fw)) {

            jw.beginArray();
            for (JsonObject obj : channels) {
                new Gson().toJson(obj, jw);
            }
            jw.endArray();
        }

        // Atomic replace — works on Windows, macOS, Linux
        Files.move(
            tmp.toPath(),
            file.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE
        );
    }
}
