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
    private volatile DeskProvider deskModelProvider;
    private static boolean debug = false;
    private int consecutiveFailures = 0;
    private static final int FAILURE_THRESHOLD = 3;
    private static final long COOLDOWN_MS = 15000;
    private long lastRecoveryTime = 0;
    private final MidiIOManager ioManager;
    private boolean everConnected = false;

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
            checkLiveness();
        }
    }

    void checkLiveness() {
        String deskType = deskModelProvider.getDeskType();
        if (deskType.isEmpty()) {
            logger.warning("Desk type is empty");
            return;
        }
        if (debug) logger.fine(String.format("Checking liveness of %s", deskType));
        if (!ioManager.hasValidDevices()) {
            consecutiveFailures = 0;
            NotifyClients.publish(ServerEvent.warning(
                    "LIVENESS", String.format("Desk %s  is not connected", deskType),
                    isConnectedObject(false)));
            return;
        }
        boolean isAlive = deskDiscovery.probeForLiveness(deskType);
        if (isAlive) {
            if (debug) logger.fine("Still alive");
            NotifyClients.publish(
                ServerEvent.info(
                    "LIVENESS",
                    String.format("Desk %s  is connected", deskType),
                    isConnectedObject(true)
                )
            );
            if (!everConnected) everConnected = true;
            if (consecutiveFailures > 0) consecutiveFailures = 0;
            return;
        }
        logger.warning("Not connected");
        consecutiveFailures++;
        if (consecutiveFailures >= FAILURE_THRESHOLD) {
            long now = System.currentTimeMillis();
            if (now - lastRecoveryTime > COOLDOWN_MS) {
                logger.warning("Liveness threshold exceeded - triggering MIDI subsystem reset");
                ioManager.resetMidiSubsystem();
                lastRecoveryTime = now;
            }
            consecutiveFailures = 0;
        }
        NotifyClients.publish(
            ServerEvent.warning(
                "LIVENESS",
                String.format("Desk %s  is not connected", deskType),
                isConnectedObject(false)
            )
        );
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
