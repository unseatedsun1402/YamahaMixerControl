package MidiControl.TestUtilities;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class TestFileUtils {

    /**
     * Creates a temporary JSON file with the given content.
     * The file is marked for deletion on JVM exit.
     */
    public static File createTempJson(String jsonContent) throws IOException {
        File tmp = File.createTempFile("test-json-", ".json");
        tmp.deleteOnExit();

        try (FileWriter writer = new FileWriter(tmp, StandardCharsets.UTF_8)) {
            writer.write(jsonContent);
        }

        return tmp;
    }

    /**
     * Reads the entire file as a UTF-8 string.
     */
    public static String readFile(File file) throws IOException {
        return Files.readString(file.toPath(), StandardCharsets.UTF_8);
    }

    /**
     * Writes a UTF-8 string to a file.
     */
    public static void writeFile(File file, String content) throws IOException {
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
    }
}