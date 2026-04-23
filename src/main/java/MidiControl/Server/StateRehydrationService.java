package MidiControl.Server;

import java.util.logging.*;

import MidiControl.MidiDeviceManager.MidiIOManager;
import MidiControl.Server.Protocol.NotifyClients;
import MidiControl.Server.Protocol.ServerEvent;

public class StateRehydrationService implements RehydrationListener{

    private final RehydrationManager rehydrationManager;
    private final MidiIOManager ioManager;
    private static Logger logger = Logger.getLogger("Rehydration API");

    public StateRehydrationService(RehydrationManager rehydrationManager, MidiIOManager io) {
        this.rehydrationManager = rehydrationManager;
        this.ioManager = io;
    }

    public void rehydrate() {
        if (!ioManager.hasValidDevices()) {
            logger.warning("Rehydrate called but skipped: no valid MIDI devices");
            NotifyClients.publish(ServerEvent.info("REHYDRATION", "Rehydration failed - no valid MIDI devices open"));
            return;
        }

        logger.info("Starting rehydration");
        rehydrationManager.rehydrateAll(this);
        NotifyClients.publish(ServerEvent.info("REHYDRATION", "Rehydration Started"));
    }

    public void clearPending(){
        logger.info("Clearing any pending hydration requests");
        rehydrationManager.clearPending(this);
    }

    public void requestMeters() {
        logger.info("Request to refresh meter updates");
        NotifyClients.publish(ServerEvent.info("METERS", "Requesting meter refresh"));
        rehydrationManager.requestMeters();
    }

    @Override
    public void onFinished() {
        NotifyClients.publish(ServerEvent.info("REHYDRATION", "Rehydration Finished"));
    }

    @Override
    public void onReset() {
        NotifyClients.publish(ServerEvent.info("REHYDRATION", "Rehydration resetting"));
    }
}
