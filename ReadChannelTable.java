/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package MidiControl;
import java.io.File;
import com.google.gson.*;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;


/**
 *
 * @author ethanblood
 */
public class ReadChannelTable {
    static private File file;
    
    /**
     * Sets the file path to be used for getChannelnames
     * @param path 
     */
    public static void setfile(String path){
        file = new File(path);
    }
    
    /**
     * reads the set file as a json array or channel information
     * @return List<String>
     * @throws FileNotFoundException 
     */
    public static List<String> getChannelnames() throws FileNotFoundException{
        var obj = JsonParser.parseReader(new FileReader(file));
        List<JsonElement> jsonArray = obj.getAsJsonArray().asList();
        List<String> names = new ArrayList<>();
        for(JsonElement item : jsonArray){
            names.add(item.getAsJsonObject().get("short").getAsString());
        }
        return(names);
    }
    
    public static List<String> getChannelcolors() throws FileNotFoundException{
        var obj = JsonParser.parseReader(new FileReader(file));
        List<JsonElement> jsonArray = obj.getAsJsonArray().asList();
        List<String> colors = new ArrayList<>();
        for(JsonElement item : jsonArray){
            if(item.getAsJsonObject().has("color")){
                colors.add(item.getAsJsonObject().get("color").getAsString());}
            else{
                colors.add("#ffea00");
            }
        }
        return(colors);
    }
}
