/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package MidiControl;
import jakarta.websocket.*;
import java.io.IOException;
import java.util.logging.Logger;


/**
 *
 * @author ethanblood
 */
@jakarta.websocket.server.ServerEndpoint("/endpoint")
public class Socket {
    static MidiServer ms;
    static Thread listener;
    
    public void Socket(){
        MidiServer ms = new MidiServer();
        //Thread listener = new Thread(ms);
    }
    
    
    private boolean serverexists(){
        if(!(listener == null)){
            if(!(listener.isAlive())){
                return false;
            }
        }
        return true;
    }
    
    public MidiServer getServer(){
        if(!(serverexists())){
            return ms;
        }
        return null;
    }
    
    @OnOpen
    public void onOpen(Session session) {
        System.out.println("\nconnection initiated");// Handle new connection
        
        if(!(ms == null)){
            if(!(serverexists())){
            listener.start();   
            }
        }
        else{
            ms = new MidiServer();
            ms.setrunwhile(true);
            listener = new Thread(ms);
            listener.start();
        }
        System.out.println("Server started");
    }
    
    
    @OnMessage
    public String onMessage(String message) {
        if(!(serverexists())){
            return ("Not Okay");
        }
        try{
        //ms.sendMidi(message);
        MidiServer.bufferMidi(message);
        //System.out.println(message);
        }
        catch(Exception e){
            System.out.println("Error: In sendMidi");
            System.out.println(e);
        }
        return(message);
    }
    
    //@OnError
    //public String OnError (String message) {
    //    return ("console.log(\"error!\")");
    //}
    
    @OnClose
    public void onClose(Session session) throws IOException{
        session.close();
    }
    
    
}
