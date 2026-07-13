package MidiControl.UserInterface.Meter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import MidiControl.Routing.WebSocketEndpoint;

public class MeterBroadcaster implements MeterUpdateListener{
    private static final Logger logger = Logger.getLogger(MeterBroadcaster.class.getName());
    private static Boolean debug = false;
    private static final long UPDATE_INTERVAL_MS = 200;
    private final Map<String, Long> lastSent = new ConcurrentHashMap<>();

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

        String key =
                dto.category +
                ":" +
                dto.source +
                ":" +
                dto.offset;

        long now = System.currentTimeMillis();

        Long previous = lastSent.get(key);

        if (previous != null &&
            (now - previous) < UPDATE_INTERVAL_MS) {
            return;
        }

        lastSent.put(key, now);

        broadcast(dto.toJson());
    }
}
