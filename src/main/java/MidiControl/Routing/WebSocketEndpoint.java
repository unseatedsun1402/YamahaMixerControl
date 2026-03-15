package MidiControl.Routing;

import MidiControl.ContextModel.BankContext;
import MidiControl.Server.MidiServer;
import MidiControl.Server.MidiServerListener;
import MidiControl.Server.SubscriptionManager;
import MidiControl.UserInterface.UiBankFactory;
import MidiControl.UserInterface.DTO.UiBankDTO;

import jakarta.websocket.OnOpen;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnClose;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@ServerEndpoint(value = "/endpoint")
public class WebSocketEndpoint {

    private static final Set<Session> sessions = ConcurrentHashMap.newKeySet();
    private static final Logger logger = Logger.getLogger(WebSocketEndpoint.class.getName());
    private static final Gson gson = new Gson();
    private static boolean DEBUG = false;
    private static final long TIMEOUT = 3000;

    private MidiServer server;
    private SubscriptionManager subscriptions;
    private static Map<Integer,Boolean> sessionLocks = new ConcurrentHashMap<>();

    public static void enableDebug(){
        DEBUG = true;
    }

    private static String encode(String type, Object payload) {
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("type", type);
        envelope.put("payload", payload);
        return gson.toJson(envelope);
    }

    @OnOpen
    public void onOpen(Session session) {

        server = (MidiServer) MidiServerListener.CONTEXT.getAttribute("midiServer");
        subscriptions = server.getSubscriptionManager();

        sessions.add(session);
        System.out.println("Client connected: " + session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            
            JsonElement root = JsonParser.parseString(message);

            if (!root.isJsonObject()) {
                logger.severe("Invalid message (not an object): " + message);
                return;
            }

            JsonObject json = root.getAsJsonObject();

            String type = json.get("type").getAsString();

            switch (type) {

                case "get-ui-bank": {
                    String bankId = json.getAsJsonObject("payload").get("bankId").getAsString();
                    UiBankFactory bankFactory = server.getUiBankFactory();
                    BankContext bankCtx = server.getBankCatalog().getBank(bankId);
                    UiBankDTO bankDto = bankFactory.buildBank(bankId, bankCtx);
                    send(session, encode("ui-bank", bankDto));
                    break;
                }

                default:
                    server.getServerRouter().handleMessage(session, message);
                    break;
            }

        } catch (Exception e) {
            logger.severe("Failed to process message: " + e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session) {
        subscriptions.removeSession(session);
        sessions.remove(session);
    }

    public static void send(Session session, String message) {
        try {
            sendWithTimeout(session,message);
            }
        catch (Exception e) {
            logger.warning("Failed to send message: " + e.getMessage());
        }
    }

    public static void broadcast(String message) {
        if (DEBUG) logger.info("Broadcasting message to clients "+message);
        for (Session s : sessions) {
            sendWithTimeout(s,message);
        }
    }

    private static void sendWithTimeout(Session session, String message){
        long timeout = System.currentTimeMillis() + TIMEOUT;
        while (System.currentTimeMillis()<timeout){
            if (session.isOpen()) {
                if(!lockSession(session)) continue;
                try {
                    session.getBasicRemote().sendText(message);
                    Thread.sleep(0, 100);
                    unlockSession(session);
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                unlockSession(session);
            }
        }
        logger.warning("Could not send message before timeout "+message);
    }

    private static boolean lockSession(Session session){
         return (sessionLocks.putIfAbsent(session.hashCode(), false) == null ? true : false);
    }

    private static boolean unlockSession(Session session){
        return sessionLocks.remove(session.hashCode());
    }
}
