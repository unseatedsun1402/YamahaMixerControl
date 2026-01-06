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

    /**
     * Public entry point for triggering a full desk rehydration.
     * Safe to call after device changes, reconnects, or user‑initiated sync.
     */
    public void rehydrate() {
        // Defensive: ensure devices are valid before attempting hydration
        if (!ioManager.hasValidDevices()) {
            logger.info("Rehydrate skipped: no valid MIDI devices");
            return;
        }

        logger.info("Starting rehydration");

        // Delegate to the rehydration manager (which handles pacing, batching, etc.)
        rehydrationManager.rehydrateAll();
    }

    public void clearPending(){
        logger.info("Clearing any pending hydration requests");
        rehydrationManager.clearPending();
    }

    // Other façade methods…
}
