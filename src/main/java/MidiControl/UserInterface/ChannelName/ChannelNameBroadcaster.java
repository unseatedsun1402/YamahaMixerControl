package MidiControl.UserInterface.ChannelName;

import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import MidiControl.Routing.WebSocketEndpoint;

public class ChannelNameBroadcaster implements ChannelNameListener{
    private static final Logger logger = Logger.getLogger(ChannelNameBroadcaster.class.getName());
    private static Boolean debug = false;
    private static Gson gsonReader = new Gson();

    public static void enableDebug(){
        debug = false;
    }

    private void broadcast(String json) {
        try {
            if (debug){logger.info("Broadcasting update "+json);}
            WebSocketEndpoint.broadcast(json);
        } catch (Exception e) {
            logger.severe("Exception thrown broadcasting the update: "+json);
        }
    }

    public static String toJson(String channelContext, String updatedName) throws JsonSyntaxException {
        String json = "{\"type\":\"name-update\",\"payload\":{\"context-id\":\""+channelContext+"\",\"name\":\""+updatedName+"\"}}";
        try {
            gsonReader.fromJson(json,JsonObject.class);
            return json;
        }
        catch (JsonSyntaxException e){ logger.severe("Cannot broadcast message of invalid json: ");
         throw new JsonSyntaxException("Cannot broadcast message of invalid json: "+json);}
    }

    @Override
    public void onChannelNameUpdated(String contextId, String updatedName) {
        if (debug) logger.finer("Broadcasting name update "+updatedName);
        broadcast(toJson(contextId, updatedName));
    }
}
