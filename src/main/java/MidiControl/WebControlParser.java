/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package MidiControl;
//import org.json.*;
import com.google.gson.*;
import javax.sound.midi.ShortMessage;


/**
 *
 * @author ethanblood
 */
public class WebControlParser {
    public static ShortMessage[] parseJSON(String message){
        JsonObject jsonobject = JsonParser.parseString(message).getAsJsonObject();
        ShortMessage[] command = new ShortMessage[4];
        for(int i = 0; i < command.length ; i++){
            command[i] = new ShortMessage();
        }
            
        int param = jsonobject.get("param").getAsInt();
        int pbyte;
        int id = Integer.parseInt(jsonobject.get("id").getAsString().substring(2));
        
        pbyte = ((128*param)+id) / 128;
        int lbyte = ((128*param)+id) % 128;
        
        int value = jsonobject.get("value").getAsInt();
        try{
            command[0].setMessage(ShortMessage.CONTROL_CHANGE, 0x62, lbyte);
        }
        catch (javax.sound.midi.InvalidMidiDataException e){
            System.err.println(e+"Invalid Midi: \n"+0);
        }
        
        // cc 99
        try{
            command[1].setMessage(ShortMessage.CONTROL_CHANGE, 0x63, pbyte);
        }
        catch (javax.sound.midi.InvalidMidiDataException e){
            System.err.println(e+"Invalid Midi: \n"+param);
        }
        
        // cc 6
        try{
            command[2].setMessage(ShortMessage.CONTROL_CHANGE, 6, value);
        }
        catch (javax.sound.midi.InvalidMidiDataException e){
            System.err.println(e+"Invalid Midi: \n"+value);
        }
        
        try{
            command[3].setMessage(ShortMessage.CONTROL_CHANGE, 26, value);
        }
        catch (javax.sound.midi.InvalidMidiDataException e){
            System.err.println(e+"Invalid Midi: \n"+value);
        }
        
        
        // cc 38
        // not needed unless 1024 point precision
        
        return command;
    }
}
