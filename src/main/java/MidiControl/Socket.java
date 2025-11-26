package MidiControl;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import MidiControl.NrpnUtils.NrpnParser;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
/**
 *
 * @author ethanblood
 */
@jakarta.websocket.server.ServerEndpoint("/endpoint")
public class Socket {
    private static final Set<Session> sessions = ConcurrentHashMap.newKeySet();
    private static final Map<Session, String> sceneMap = new ConcurrentHashMap<>();
    private static final MidiServer ms = new MidiServer();
    private static Thread listener = null;
    private static Thread sendThread = null;

    public synchronized MidiServer getServer() {
        if (listener == null) {
            listener = new Thread(ms);
            listener.start();
            Logger.getLogger(Socket.class.getName()).log(Level.INFO, "MidiServer thread created and started from socket.");
        } else if (!listener.isAlive()) {
            Logger.getLogger(Socket.class.getName()).log(Level.WARNING, "MidiServer thread exists but is not alive. Not restarting.");
        } else {
            Logger.getLogger(Socket.class.getName()).log(Level.INFO, "MidiServer thread already running.");
        }

        NrpnParser.setDispatchTarget(ms);

        if (MidiServer.midiOut != null) {
            SyncSend sender = new SyncSend(MidiServer.outputBuffer);
            sendThread = new Thread(sender);
            sendThread.start();
            Logger.getLogger(Socket.class.getName()).log(Level.INFO, "SyncSend thread started from socket.");
        } else {
            Logger.getLogger(Socket.class.getName()).log(Level.WARNING,"Output device (midiOut) is null — SyncSend not started.");
            Logger.getLogger(Socket.class.getName()).log(Level.INFO,"Input device (midiIn) transmitter attached: " + (MidiServer.midiIn != null));


        }
            Logger.getLogger(Socket.class.getName()).log(Level.INFO, "SyncSend thread already running.");
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

    public static synchronized void restartSyncSend() {
        if (sendThread != null && sendThread.isAlive()) {
            sendThread.interrupt(); // stop old thread
            Logger.getLogger(Socket.class.getName()).log(Level.INFO, "SyncSend thread interrupted.");
        }

        if (MidiServer.midiOut != null) {
            SyncSend sender = new SyncSend(MidiServer.outputBuffer);
            sendThread = new Thread(sender);
            sendThread.start();
            Logger.getLogger(Socket.class.getName()).log(Level.INFO, "SyncSend thread restarted.");
        } else {
            Logger.getLogger(Socket.class.getName()).log(Level.WARNING, "Receiver is null — SyncSend not restarted.");
        }
    }

    public static void broadcast(String message) {
    for (Session session : sessions) {
        if (session.isOpen()) {
            try {
                Logger.getLogger(Socket.class.getName()).log(Level.FINE, "Broadcasting message to session " + session.getId() + ": " + message);
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                Logger.getLogger(Socket.class.getName()).log(Level.SEVERE, new String("Error broadcasting to session " + session.getId() + " "+ e.getClass().getName() + ": " + e.getMessage()));
                e.printStackTrace();
            }
        }
    }
}
}