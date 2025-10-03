package MidiControl;
import jakarta.websocket.*;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;


/**
 *
 * @author ethanblood
 */
@jakarta.websocket.server.ServerEndpoint("/endpoint")
public class Socket {
    private static final Set<Session> sessions = ConcurrentHashMap.newKeySet();
    private static final Map<Session, String> sceneMap = new ConcurrentHashMap<>();
    private static final MidiServer ms = new MidiServer();
    static final Thread listener = new Thread(ms);
   private static Thread sendThread;

public synchronized MidiServer getServer() {
    if (listener == null || !listener.isAlive()) {
        listener.start();
        Logger.getLogger(Socket.class.getName()).log(java.util.logging.Level.INFO, "MidiServer thread started.");
    }

    if (sendThread == null || !sendThread.isAlive()) {
        if (MidiServer.rcvr != null) {
            SyncSend sender = new SyncSend(MidiServer.buffer, MidiServer.rcvr);
            sendThread = new Thread(sender);
            sendThread.start();
            Logger.getLogger(Socket.class.getName()).log(java.util.logging.Level.INFO, "SyncSend thread started.");
        } else {
            Logger.getLogger(Socket.class.getName()).log(java.util.logging.Level.WARNING, "Receiver is null — SyncSend not started.");
        }
    }

    return ms;
}

    
    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        sceneMap.put(session, "default");
        System.out.println("Client connected: " + session.getId());

        getServer(); // This now starts the thread if needed

        Logger.getLogger(Socket.class.getName()).log(java.util.logging.Level.INFO, "MidiServer is active.");
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        sceneMap.remove(session);
        System.out.println("Client disconnected: " + session.getId());
    }
    
    
   @OnMessage
    public String onMessage(String message, Session session) {
        System.out.println("Received from GUI: " + message);
        try {
            MidiServer.bufferMidi(message);
        } catch (Exception e) {
            System.out.println("Error in bufferMidi:");
            e.printStackTrace();
        }
        return message;
    }

    public static void broadcast(String message) {
    for (Session session : sessions) {
        if (session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
}