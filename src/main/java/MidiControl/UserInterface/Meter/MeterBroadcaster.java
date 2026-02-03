package MidiControl.UserInterface.Meter;

import java.util.logging.Level;
import java.util.logging.Logger;

import MidiControl.Routing.WebSocketEndpoint;

public class MeterBroadcaster implements MeterUpdateListener{
    private static final Logger logger = Logger.getLogger(MeterBroadcaster.class.getName());
    private static Boolean DEBUG = false;

    public static void enableDebug(){
        DEBUG = true;
    }

    private void broadcast(String json) {
        try {
            if (DEBUG){logger.fine("Broadcasting update");}
            WebSocketEndpoint.broadcast(json);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception thrown broadcasting the update: " + json, e);
        }
    }

    @Override
    public void onMeterUpdate(MeterDTO dto) {
        if (DEBUG){logger.fine("Meter update: " + dto.toJson());}
        broadcast(dto.toJson());
    }
    
}
