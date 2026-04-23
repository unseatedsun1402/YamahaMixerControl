package MidiControl.Server.Protocol;

import com.google.gson.JsonObject;

import MidiControl.Routing.WebSocketEndpoint;

public class NotifyClients implements ServerEventPublisher{

    public static void publish(ServerEvent event) {
        JsonObject json = ServerEventSerializer.toJson(event);
        WebSocketEndpoint.broadcast(json.toString());
    }
    
}
