package MidiControl.LivenessMonitor;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import com.google.gson.JsonObject;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.DeskProvider;
import MidiControl.DeskDiscovery.DeskDiscovery;
import MidiControl.MidiDeviceManager.MidiIOManager;
import MidiControl.Server.Protocol.NotifyClients;
import MidiControl.Server.Protocol.ServerEvent;

public class DeviceLivenessMonitor{

    private final DeskDiscovery deskDiscovery;
    private Timer timer;
    private static final long CHECK_INTERVAL = 5000; // 5 seconds
    private static final Logger logger = Logger.getLogger(DeviceLivenessMonitor.class.getName());
    private DeskProvider deskModelProvider;
    private static boolean debug = false;
    private int consecutiveFailures = 0;
    private static final int FAILURE_THRESHOLD = 3;
    private static final long COOLDOWN_MS = 15000;
    private long lastRecoveryTime = 0;
    private final MidiIOManager ioManager;

    public DeviceLivenessMonitor(DeskDiscovery deskDiscovery, DeskProvider canonicalRegistry, MidiIOManager manager) {
        this.deskDiscovery = deskDiscovery;
        this.deskModelProvider = canonicalRegistry;
        this.timer = new Timer();
        this.ioManager = manager;
    }

    public void startMonitoring() {
        timer.scheduleAtFixedRate(new LivenessCheckTask(), 0, CHECK_INTERVAL);
    }

    public void stopMonitoring() {
        if (timer != null) {
            timer.cancel();
        }
    }

    private class LivenessCheckTask extends TimerTask {
        @Override
        public void run() {
            if(deskModelProvider.getDeskType().isEmpty()) {logger.warning("Desk type is empty"); return;}
            if(debug)logger.fine(String.format("Checking liveness of %s",deskModelProvider.getDeskType()));
            boolean isAlive = deskDiscovery.probeForLiveness(deskModelProvider.getDeskType());
            if (isAlive) {
                if(debug)logger.fine("Still alive");
                NotifyClients.publish(
                    ServerEvent.info(
                        "LIVENESS",
                        String.format("Desk %s  is connected", deskModelProvider.getDeskType()),
                        isConnectedObject(true)
                    )
                );
                if(consecutiveFailures > 0) consecutiveFailures = 0;
            } else {
                logger.warning("Not connected");
                consecutiveFailures ++;
                if (consecutiveFailures >= FAILURE_THRESHOLD) {
                    long now = System.currentTimeMillis();
                    if (now - lastRecoveryTime > COOLDOWN_MS) {

                        logger.warning("Liveness threshold exceeded - triggering MIDI subsystem reset");

                        ioManager.resetMidiSubsystem();
                        lastRecoveryTime = now;
                    }
                }
                NotifyClients.publish(
                    ServerEvent.warning(
                        "LIVENESS",
                        String.format("Desk %s  is not connected", deskModelProvider.getDeskType()),
                        isConnectedObject(false)
                    )
                );
            }
        }
    }

    public void injectNewRegistry(CanonicalRegistry newRegistry) {
        logger.info("Registry reloaded @"+newRegistry.hashCode());
        this.deskModelProvider = newRegistry;
    }

    public static void enableDebug(){
        debug = true;
    }

    private JsonObject isConnectedObject(boolean isConnected) {
        JsonObject obj = new JsonObject();
        obj.addProperty("isConnected", isConnected);
        return obj;
    }
}
