/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package MidiControl;
import jakarta.websocket.*;
import java.io.IOException;


/**
 *
 * @author ethanblood
 */
@jakarta.websocket.server.ServerEndpoint("/endpoint")
public class Socket {
    static MidiServer ms;
    static Thread listener;
    
    public Socket(){
        MidiServer ms = new MidiServer();
        ms.run();
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
        MidiServer.bufferMidi(message);
        }
        catch(Exception e){
            System.out.println("Error: In sendMidi");
            System.out.println(e);
        }
        return(message);
    }
    

    @OnClose
    public void onClose(Session session) throws IOException{
        session.close();
    }
    
    
}
