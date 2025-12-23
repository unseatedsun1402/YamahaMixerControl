package MidiControl.MidiDeviceManager.Configs;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class ChannelNameStoreInitializer {

    private static final String FILE_NAME = "channels.json";

    public static File initialize() throws IOException {
        Path baseDir = ConfigDirectoryResolver.resolveBaseDirectory();
        Files.createDirectories(baseDir);

        Path filePath = baseDir.resolve(FILE_NAME);
        File file = filePath.toFile();

        if (!file.exists()) {
            Files.writeString(
                filePath,
                "[]", // your store expects a JSON array
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW
            );
        }

        return file;
    }
}