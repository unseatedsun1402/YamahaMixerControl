package MidiControl.Routing;

import MidiControl.ContextModel.BankContext;
import MidiControl.Server.MidiServer;
import MidiControl.Server.MidiServerListener;
import MidiControl.Server.SubscriptionManager;
import MidiControl.UserInterface.UiBankFactory;
import MidiControl.UserInterface.UiModelFactory;
import MidiControl.UserInterface.DTO.UiBankDTO;
import MidiControl.UserInterface.DTO.UiModelDTO;

import jakarta.websocket.OnOpen;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnClose;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.google.gson.Gson;

@ServerEndpoint(value = "/endpoint")
public class WebSocketEndpoint {

    private static final Set<Session> sessions = ConcurrentHashMap.newKeySet();
    private static final Logger logger = Logger.getLogger(WebSocketEndpoint.class.getName());

    private static final Gson gson = new Gson();

    private MidiServer server;
    private SubscriptionManager subscriptions;

    // ---------------------------------------------------------
    // Local JSON envelope (no external dependency)
    // ---------------------------------------------------------
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

        // ---------------------------------------------------------
        // UI BOOTSTRAP SEQUENCE
        // ---------------------------------------------------------

        UiBankFactory bankFactory = server.getUiBankFactory();
        UiModelFactory modelFactory = server.getUiModelFactory();

        String bankId = "bank.inputs";
        BankContext bankCtx = server.getBankCatalog().getBank(bankId);

        UiBankDTO bankDto = bankFactory.buildBank(bankId, bankCtx);
        send(session, encode("ui-bank", bankDto));

        for (String ctxId : bankDto.contexts) {
            UiModelDTO model = modelFactory.buildUiModel(ctxId);
            send(session, encode("ui-model", model));
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        server.getServerRouter().handleMessage(session, message);
    }

    @OnClose
    public void onClose(Session session) {
        subscriptions.removeSession(session);
        sessions.remove(session);
    }

    // ---------------------------------------------------------
    // Sending helpers
    // ---------------------------------------------------------

    public static void send(Session session, String message) {
        try {
            if (session.isOpen()) {
                session.getBasicRemote().sendText(message);
            }
        } catch (Exception e) {
            logger.warning("Failed to send message: " + e.getMessage());
        }
    }

    public static void broadcast(String message) {
        for (Session s : sessions) {
            send(s, message);
        }
    }
}