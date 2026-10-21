package MidiControl.UserInterface.Meter;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import MidiControl.Routing.WebSocketEndpoint;

public class MeterBroadcaster implements MeterUpdateListener{
    private static final Logger logger = Logger.getLogger(MeterBroadcaster.class.getName());
    private static Boolean debug = false;

    public static void enabledebug(){
        debug = true;
    }

    private void broadcast(String json) {
        try {
            if (debug){logger.fine("Broadcasting update");}
            WebSocketEndpoint.broadcast(json);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception thrown broadcasting the update: " + json, e);
        }
    }

    @Override
    public void onMeterUpdate(MeterDTO dto) {
        broadcast(dto.toJson());
    }

    @Override
    public void onMeterUpdates(List<MeterDTO> updates) {
        if (updates.isEmpty()) {
            return;
        }
        broadcast(MeterBatchSerializer.toJson(updates));
    }
}
