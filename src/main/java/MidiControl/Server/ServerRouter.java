package MidiControl.Server;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import MidiControl.ControlServer.GuiInputHandler;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;
import MidiControl.MidiDeviceManager.MidiDeviceDTO;
import MidiControl.MidiDeviceManager.MidiIOManager;
import MidiControl.MidiDeviceManager.ServerSettings;
import MidiControl.MidiDeviceManager.Settings;
import MidiControl.Routing.OutputRouter;
import MidiControl.Routing.WebSocketEndpoint;
import MidiControl.SysexUtils.MappingFiles;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;
import MidiControl.UserInterface.UiModelService;
import MidiControl.UserInterface.ChannelName.ChannelNameAssembler;
import MidiControl.UserInterface.ChannelName.ChannelNameBroadcaster;
import MidiControl.UserInterface.DTO.UiModelDTO;
import jakarta.websocket.Session;

public class ServerRouter {

    private final UiModelService uiModels;
    private final Gson gson = new Gson();
    private final SubscriptionManager subscriptions;
    private final GuiInputHandler guiInputHandler;
    private final OutputRouter outputRouter;
    private final int TIMEOUT_MS = 10000;
    private int lastKeepAlive = 0;
    private static final Logger logger = Logger.getLogger(ServerRouter.class.getName());
    private final MidiIOManager ioManager;
    private CanonicalRegistry registry;
    private App app;
    private Settings serverSettings = new ServerSettings();

    public ServerRouter(UiModelService uiModels,
                        SubscriptionManager subscriptions,
                        CanonicalRegistry registry,
                        MidiIOManager ioManager)
    {
        this.uiModels = uiModels;
        this.subscriptions = subscriptions;
        this.registry = registry;
        this.ioManager = ioManager;
        this.outputRouter = new OutputRouter(registry, this.ioManager);
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
        JsonElement payloadElement = root.get("payload");
        JsonObject payload = payloadElement != null && payloadElement.isJsonObject()
                ? payloadElement.getAsJsonObject()
                : new JsonObject(); // fallback

        switch (type) {
            case "get-ui-model" -> handleGetUiModel(session, requestId, payload);
            case "set-control-value" -> handleSetControlValue(session, requestId, payload);
            case "subscribe-context" -> handleSubscribe(session, requestId, payload);
            case "unsubscribe-context" -> handleUnsubscribe(session, requestId, payload);
            case "list-midi-devices" -> handleListMidiDevices(session, requestId);
            case "set-midi-device" -> handleSetMidiDevice(session, requestId, payload);
            case "apply-midi-settings" -> handleApplyMidiSettings(session, requestId, payload);
            case "save-midi-settings" -> handleSaveMidiSettings(session, requestId, payload);
            case "meter-keep-alive" -> handleMeterKeepAlive(session,requestId,payload);
            case "request-channel-names" -> handleRequestChannelNames(session,requestId,payload);
            default -> sendError(session, requestId, "UNKNOWN_TYPE", "Unknown message type: " + type);
        }
    }

    private void handleRequestChannelNames(Session session, String requestId, JsonObject payload) {
        logger.info("Channel name request: " + requestId);
        Map<String,String> knownNames = ChannelNameAssembler.getChannelNames();
        Set<String> keys = knownNames.keySet();
        for (String key : keys){
            WebSocketEndpoint.send(session, ChannelNameBroadcaster.toJson(key,knownNames.get(key)));
        }
    }

    private void handleMeterKeepAlive(Session session, String requestId, JsonObject payload) {
        int messageTime = payload.get("timestamp").getAsInt();
        if ((messageTime - lastKeepAlive) > TIMEOUT_MS){app.requestMeters();}
        else{lastKeepAlive = messageTime;}
    }

    private void handleGetUiModel(Session session, String requestId, JsonObject payload) {
        String contextId = payload.get("contextId").getAsString();
        String uiType = payload.has("uiType")
                ? payload.get("uiType").getAsString()
                : "basic-input-view";

        UiModelDTO model;
        model = uiModels.buildUiModel(contextId, uiType);

        JsonObject response = new JsonObject();
        response.addProperty("type", "ui-model");
        if (requestId != null) response.addProperty("requestId", requestId);

        JsonObject out = new JsonObject();
        out.addProperty("contextId", model.contextId);
        out.add("controls", gson.toJsonTree(model.controls));
        out.add("metadata", gson.toJsonTree(model.metadata));
        response.add("payload", out);

        WebSocketEndpoint.send(session, response.toString());
    }

    private void handleSetControlValue(Session session, String requestId, JsonObject payload) {
        String canonicalId = payload.get("canonicalId").getAsString();
        int value = payload.get("value").getAsInt();
        logger.fine("Update from "+canonicalId+" val: "+value);

        ControlInstance ci = registry.resolveCanonicalId(canonicalId);
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

    private boolean handleApplyMidiSettings(Session session, String requestId, JsonObject payload) {
        logger.info("Applying new settings "+payload.toString());
        int newInput = payload.get("inputDeviceId").getAsInt();
        int newOutput = payload.get("outputDeviceId").getAsInt();
        String mappingString = payload.get("consoleType").getAsString();

        // Apply output device
        boolean outOk = ioManager.trySetOutputDevice(newOutput);

        // Apply input device
        boolean inOk = ioManager.trySetInputDevice(newInput);
        
        List<SysexMapping> newMappings = SysexMappingLoader.loadMappingsFromResource(MappingFiles.getFilePathByKey(mappingString));
        if (newMappings != null ){
            this.registry.reloadMappings(newMappings, new SysexParser(newMappings));
            logger.info("New registry loaded "+ mappingString);
        } 

        if (ioManager.hasValidDevices()) {
            if(app == null){logger.severe("App for rehydration is null - cannot rehydrate");}
            else { app.rehydrate(); }
        }

        // --- ACK ---
        JsonObject ack = new JsonObject();
        ack.addProperty("type", "ack");
        if (requestId != null) ack.addProperty("requestId", requestId);

        JsonObject p = new JsonObject();
        p.addProperty("type", "apply-settings");
        p.addProperty("midi_status", (inOk && outOk) ? "ok" : "error");
        p.addProperty("mapping_status", (newMappings != null ) ? "ok" : "error");
        ack.add("payload", p);

        WebSocketEndpoint.send(session, ack.toString());
        return ( inOk && outOk && (newMappings != null) );
    }

    private void handleSaveMidiSettings(Session session, String requestId, JsonObject payload) {
        // --- Required settings (supported now) ---
        logger.info("Saving settings "+payload.toString());
        int newInput = payload.get("inputDeviceId").getAsInt();
        int newOutput = payload.get("outputDeviceId").getAsInt();
        String consoleName = payload.get("consoleType").getAsString();

        if(handleApplyMidiSettings(session, requestId, payload)){
                serverSettings.newSettings(newInput,                // in index
                ioManager.getMidiIn().getDeviceInfo().getName(),    // in name
                newOutput,                                          // out index
                ioManager.getMidiOut().getDeviceInfo().getName(),   // out name
                consoleName);                                       // console name
            serverSettings.saveSettings();
        }
        else{logger.warning("Failed to save settings, configuration not accepted: "+payload);}
    }

    public OutputRouter getOutputRouter() {
        return outputRouter;
    }
}