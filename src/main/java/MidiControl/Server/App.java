package MidiControl.Server;

import java.util.logging.*;

import MidiControl.MidiDeviceManager.MidiIOManager;

public class App {

    private final RehydrationManager rehydrationManager;
    private final MidiIOManager ioManager;
    private static Logger logger = Logger.getLogger("Rehydration API");

    public App(RehydrationManager rehydrationManager, MidiIOManager io) {
        this.rehydrationManager = rehydrationManager;
        this.ioManager = io;
    }

    public void rehydrate() {
        if (!ioManager.hasValidDevices()) {
            logger.warning("Rehydrate called but skipped: no valid MIDI devices");
            return;
        }

        logger.info("Starting rehydration");

        rehydrationManager.rehydrateAll();
    }

    public void clearPending(){
        logger.info("Clearing any pending hydration requests");
        rehydrationManager.clearPending();
    }

    public void requestMeters() {
        logger.info("Request to refresh meter updates");
        rehydrationManager.requestMeters();
    }
}
