package MidiControl.Server.Protocol;

import MidiControl.Server.EventStream.EventObject;

import com.google.gson.JsonObject;

import MidiControl.Routing.WebSocketEndpoint;

public class NotifyClients implements ServerEventPublisher{

    public static void publish(ServerEvent event) {
        JsonObject json = EventObject.fromServerEvent(event);
        WebSocketEndpoint.broadcast(json.toString());
    }
    
}
