package MidiControl.Server;

import java.util.List;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import MidiControl.ControlServer.GuiInputHandler;
import MidiControl.Controls.ControlInstance;
import MidiControl.MidiDeviceManager.MidiDeviceDTO;
import MidiControl.MidiDeviceManager.MidiIOManager;
import MidiControl.Routing.OutputRouter;
import MidiControl.Routing.WebSocketEndpoint;
import MidiControl.UserInterface.UiModelFactory;
import MidiControl.UserInterface.DTO.UiModelDTO;
import jakarta.websocket.Session;

public class ServerRouter {

    private final UiModelFactory uiFactory;
    private final MidiServer midi;
    private final Gson gson = new Gson();
    private final SubscriptionManager subscriptions;
    private final GuiInputHandler guiInputHandler;
    private final OutputRouter outputRouter;
    private static final Logger logger = Logger.getLogger(ServerRouter.class.getName());
    private final MidiIOManager ioManager;
    private App app;

    public ServerRouter(MidiServer server) 
    {
        this.midi = server;
        this.uiFactory = midi.getUiModelFactory();
        this.ioManager = midi.getMidiDeviceManager();
        this.subscriptions = midi.getSubscriptionManager();
        this.outputRouter = new OutputRouter(midi.getCanonicalRegistry(),this.ioManager);
        this.guiInputHandler = new GuiInputHandler(outputRouter);
        this.app = null;
    }

    public ServerRouter(UiModelFactory uiFactory, MidiServer server, SubscriptionManager subscriptions) 
    {
        this.uiFactory = uiFactory;
        this.midi = server;
        this.ioManager = midi.getMidiDeviceManager();
        this.subscriptions = subscriptions;

        this.outputRouter = new OutputRouter(midi.getCanonicalRegistry(),this.ioManager);
        this.guiInputHandler = new GuiInputHandler(outputRouter);
        this.app = null;
    }

    public void injectApp(App app){
        this.app = app;
    }

    public void handleMessage(Session session, String message) {
        JsonObject root = gson.fromJson(message, JsonObject.class);

        String type = root.get("type").getAsString();
        String requestId = root.has("requestId") ? root.get("requestId").getAsString() : null;
        JsonObject payload = root.getAsJsonObject("payload");

        switch (type) {
            case "get-ui-model" -> handleGetUiModel(session, requestId, payload);
            case "set-control-value" -> handleSetControlValue(session, requestId, payload);
            case "subscribe-context" -> handleSubscribe(session, requestId, payload);
            case "unsubscribe-context" -> handleUnsubscribe(session, requestId, payload);
            case "list-midi-devices" -> handleListMidiDevices(session, requestId);
            case "set-midi-device" -> handleSetMidiDevice(session, requestId, payload);
            case "apply-midi-settings" -> handleApplyMidiSettings(session, requestId, payload);
            default -> sendError(session, requestId, "UNKNOWN_TYPE", "Unknown message type: " + type);
        }
    }

    private void handleGetUiModel(Session session, String requestId, JsonObject payload) {
        String contextId = payload.get("contextId").getAsString();

        UiModelDTO model = uiFactory.buildUiModel(contextId);

        JsonObject response = new JsonObject();
        response.addProperty("type", "ui-model");
        if (requestId != null) response.addProperty("requestId", requestId);

        JsonObject out = new JsonObject();
        out.addProperty("contextId", contextId);
        out.add("model", gson.toJsonTree(model));

        response.add("payload", out);

        WebSocketEndpoint.send(session, response.toString());
    }

    private void handleSetControlValue(Session session, String requestId, JsonObject payload) {
        String canonicalId = payload.get("canonicalId").getAsString();
        int value = payload.get("value").getAsInt();
        logger.fine("Update from "+canonicalId+" val: "+value);

        ControlInstance ci = midi.getCanonicalRegistry().resolveCanonicalId(canonicalId);
        if (ci != null) {
            ci.updateValue(value);
            guiInputHandler.handleGuiChange(canonicalId, value);
            // subscriptions.broadcastControlUpdate(canonicalId, value);
        } else {
            logger.warning("Unknown canonicalId in set-control-value: " + canonicalId);
        }

        // 4. Ack
        JsonObject ack = new JsonObject();
        ack.addProperty("type", "ack");
        if (requestId != null) {
            ack.addProperty("requestId", requestId);
        }
        JsonObject payloadObj = new JsonObject();
        payloadObj.addProperty("status", "ok");
        ack.add("payload", payloadObj);

        WebSocketEndpoint.send(session, ack.toString());
    }

    private void handleSubscribe(Session session, String requestId, JsonObject payload) {
        String contextId = payload.get("contextId").getAsString();

        subscriptions.subscribe(session, contextId);

        JsonObject ack = new JsonObject();
        ack.addProperty("type", "ack");
        if (requestId != null) ack.addProperty("requestId", requestId);

        JsonObject p = new JsonObject();
        p.addProperty("status", "ok");
        ack.add("payload", p);

        WebSocketEndpoint.send(session, ack.toString());
        logger.fine("Received subscribe request from"+ session.getId());
    }

    private void handleUnsubscribe(Session session, String requestId, JsonObject payload) {
        String contextId = payload.get("contextId").getAsString();

        subscriptions.unsubscribe(session, contextId);

        JsonObject ack = new JsonObject();
        ack.addProperty("type", "ack");
        if (requestId != null) ack.addProperty("requestId", requestId);

        JsonObject p = new JsonObject();
        p.addProperty("status", "ok");
        ack.add("payload", p);

        WebSocketEndpoint.send(session, ack.toString());
        logger.fine("Received unsubscribe request from"+ session.getId());
    }

    private void sendError(Session session, String requestId, String code, String message) {
        JsonObject root = new JsonObject();
        root.addProperty("type", "error");
        if (requestId != null) root.addProperty("requestId", requestId);

        JsonObject payload = new JsonObject();
        payload.addProperty("code", code);
        payload.addProperty("message", message);

        root.add("payload", payload);

        WebSocketEndpoint.send(session, root.toString());
        logger.warning("Router error: " + code + " - " + message);
    }

    private void handleListMidiDevices(Session session, String requestId) {
        List<MidiDeviceDTO> devices = ioManager.listDeviceDTOs();

        JsonObject response = new JsonObject();
        response.addProperty("type", "midi-device-list");
        if (requestId != null) response.addProperty("requestId", requestId);

        JsonArray arr = new JsonArray();
        for (MidiDeviceDTO d : devices) {
            JsonObject o = new JsonObject();
            o.addProperty("id", d.id);
            o.addProperty("name", d.name);
            o.addProperty("description", d.description);
            o.addProperty("vendor", d.vendor);
            o.addProperty("version", d.version);
            o.addProperty("canInput", d.canInput);
            o.addProperty("canOutput", d.canOutput);
            arr.add(o);
        }

        JsonObject payload = new JsonObject();
        payload.add("devices", arr);
        response.add("payload", payload);

        WebSocketEndpoint.send(session, response.toString());
    }

    private void handleSetMidiDevice(Session session, String requestId, JsonObject payload) {
        int index = payload.get("deviceId").getAsInt();

        boolean ok = ioManager.trySetOutputDevice(index);

        JsonObject ack = new JsonObject();
        ack.addProperty("type", "ack");
        if (requestId != null) ack.addProperty("requestId", requestId);

        JsonObject p = new JsonObject();
        p.addProperty("status", ok ? "ok" : "error");
        ack.add("payload", p);

        WebSocketEndpoint.send(session, ack.toString());

        if (ok)
            logger.info("Session[" + session.getId() + "] selected MIDI output device index " + index);
        else
            logger.warning("Session[" + session.getId() + "] failed to select MIDI output device index " + index);
    }

    private void handleApplyMidiSettings(Session session, String requestId, JsonObject payload) {

        // --- Required settings (supported now) ---
        int newInput = payload.get("inputDeviceId").getAsInt();
        int newOutput = payload.get("outputDeviceId").getAsInt();

        // Apply output device
        boolean outOk = ioManager.trySetOutputDevice(newOutput);

        // Apply input device
        boolean inOk = ioManager.trySetInputDevice(newInput);

        if (inOk && outOk) {
            if(app.equals(null)){logger.severe("App for rehydration is null - cannot rehydrate");}
            app.rehydrate();
        }

        // --- Future settings (commented out for now) ---
        // String inputChannel = payload.get("inputChannel").getAsString();
        // int outputChannel = payload.get("outputChannel").getAsInt();
        // boolean enableNRPN = payload.get("enableNRPN").getAsBoolean();
        // boolean enableSYSEX = payload.get("enableSYSEX").getAsBoolean();
        // boolean enableCC = payload.get("enableCC").getAsBoolean();
        // boolean debugLogging = payload.get("debugLogging").getAsBoolean();
        // boolean showRaw = payload.get("showRaw").getAsBoolean();
        // boolean showCanonical = payload.get("showCanonical").getAsBoolean();

        // --- ACK ---
        JsonObject ack = new JsonObject();
        ack.addProperty("type", "ack");
        if (requestId != null) ack.addProperty("requestId", requestId);

        JsonObject p = new JsonObject();
        p.addProperty("status", (inOk && outOk) ? "ok" : "error");
        ack.add("payload", p);

        WebSocketEndpoint.send(session, ack.toString());
    }

    public OutputRouter getOutputRouter() {
        return outputRouter;
    }
}