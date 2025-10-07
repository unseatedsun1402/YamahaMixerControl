package MidiControl;
import java.io.File;
import com.google.gson.*;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


/**
 *
 * @author unseatedsun1402
 */
public class ReadChannelTable {
    /**
     * reads the set file as a json array or channel information
     * @return List<String>
     * @throws FileNotFoundException 
     */
    public static List<String> getChannelnames() throws FileNotFoundException{
        List<String> names = new ArrayList<>();
        try {
            InputStream is = ReadChannelTable.class.getClassLoader().getResourceAsStream("MidiControl/channels.json");
            if (is == null) {
                System.out.println("channels.json not found in classpath!");
                throw new FileNotFoundException("channels.json not found in classpath");
            }
            Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            var obj = JsonParser.parseReader(reader);
            for (JsonElement item : obj.getAsJsonArray()) {
                names.add(item.getAsJsonObject().get("short").getAsString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return(names);
    }
    
    public static List<String> getChannelcolors() throws FileNotFoundException{
        List<String> colors = new ArrayList<>();
        try {
            InputStream is = ReadChannelTable.class.getClassLoader().getResourceAsStream("MidiControl/channels.json");
            if (is == null) {
                throw new FileNotFoundException("channels.json not found in classpath");
            }
            Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            var obj = JsonParser.parseReader(reader);
            for (JsonElement item : obj.getAsJsonArray()) {
                if(item.getAsJsonObject().has("color")){
                    colors.add(item.getAsJsonObject().get("color").getAsString());
                } else {
                    colors.add("#ffea00");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return(colors);
    }
}