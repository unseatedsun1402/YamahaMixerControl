/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package MidiControl;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import static java.lang.String.format;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;

/**
 *
 * @author ethanblood
 */
public class Settings {
    private static int input;
    private static int output;
    
    /**
     * Check that if the current input and output midi connections have changed during run time
     * @param currentIn
     * @param currentOut
     * @return 
     */
    public boolean checkChanges(int currentIn,int currentOut)
    {
        if(Settings.input >= 0){
            if(currentIn != Settings.input){
                return false;
            }
        }
        if(Settings.output >= 0){
            if(currentIn != Settings.output){
                return false;
            }
        }
        return true;
    }
    
    public void setPorts(int currentIn,int currentOut){
        Settings.input = currentIn;
        Settings.output = currentOut;
    }
    
    public void writeSettings(int currentIn, int currentOut) throws IOException{
        setPorts(currentIn, currentOut);
        
        String path = new File("../").getCanonicalPath() + "/conf/settings.json";        
        
        Gson gson = new GsonBuilder().create();
        var settings = new JsonObject();
        
        settings.addProperty("input", Settings.input);
        settings.addProperty("output",Settings.output);
        
        try (Writer writer = new FileWriter(path)) {
            gson.toJson(settings, writer);
        }
        System.out.println("Success written file");
    }
    
    public int[] readSettings() throws IOException{
        String path = new File("../").getCanonicalPath() + "/conf/settings.json";
        var obj = JsonParser.parseReader(new FileReader(path));
        var settings = obj.getAsJsonObject();
        setPorts(settings.get("input").getAsInt(),settings.get("output").getAsInt());
        int[] device = {Settings.input,Settings.output};
        return device;
    }
}
