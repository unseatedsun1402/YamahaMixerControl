package MidiControl;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import MidiControl.NrpnUtils.NrpnParser;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;

@jakarta.websocket.server.ServerEndpoint("/endpoint")
public class Socket {
    private static final Set<Session> sessions = ConcurrentHashMap.newKeySet();
    private static final Map<Session, String> sceneMap = new ConcurrentHashMap<>();
    private static final MidiServer ms = new MidiServer();
    private static Thread listener = null;
    private static Thread sendThread = null;
    private static Logger logger = Logger.getLogger(Socket.class.getName());
    public static Consumer<String> testBroadcastHook = null;

    public synchronized MidiServer getServer() {
        if (listener == null) {
            listener = new Thread(ms);
            listener.start();
            logger.info( "MidiServer thread created and started from socket.");
        } else if (!listener.isAlive()) {
            logger.warning("MidiServer thread exists but is not alive. Not restarting.");
        } else {
            logger.info("MidiServer thread already running.");
        }

        NrpnParser.setDispatchTarget(ms);
        logger.info("NrpnParser dispatch target set.");

        if (MidiServer.midiOut != null) {
            SyncSend sender = new SyncSend(MidiServer.outputBuffer);
            sendThread = new Thread(sender);
            sendThread.start();
            logger.info("SyncSend thread started from socket.");
        } else {
            logger.warning("Output device (midiOut) is null — SyncSend not started.");
            logger.info("Input device (midiIn) transmitter attached: " + (MidiServer.midiIn != null));


        }
            logger.info("SyncSend thread already running.");
        return ms;
    }
    
    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        sceneMap.put(session, "default");
        logger.info("Client connected: " + session.getId());

        getServer(); // This now starts the thread if needed
        logger.info("MidiServer is active.");
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        sceneMap.remove(session);
        logger.info("Client "+ session.getId() + " disconnected");
    }
    
    
   @OnMessage
    public String onMessage(String message, Session session) {
        System.out.println("Received from GUI: " + message);
        try {
            MidiServer.bufferMidi(message);
        } catch (Exception e) {
            logger.severe("Error: buffer midi failed to process message from GUI: " + e.getMessage());
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
        logger.info("Broadcasting message: "+ message);
        // Test hook: if set, bypass WebSocket and capture the message
        if (testBroadcastHook != null) {
            logger.fine("Test broadcast hook is set. Bypassing WebSocket broadcast.");
            testBroadcastHook.accept(message);
            return;
        }

        // Production WebSocket broadcast
        for (Session session : sessions) {
            if (session.isOpen()) {
                try {
                    logger.fine("Broadcasting to session " + session.getId() + ": " + message);
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    logger.severe("Error broadcasting to session " + session.getId() + " "
                            + e.getClass().getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
}