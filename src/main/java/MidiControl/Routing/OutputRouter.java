package MidiControl.Routing;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import MidiControl.Controls.ControlInstance;
import MidiControl.MidiDeviceManager.MidiIOManager;
import MidiControl.MidiDeviceManager.TransportMode;
import MidiControl.Controls.CanonicalRegistry;;

public class OutputRouter implements OutputRequestSender{

    private static final Logger logger = Logger.getLogger(OutputRouter.class.getName());
    private static boolean DEBUG = false;

    public static void enableDebug() {
        DEBUG = true;
        logger.info("OutputRouter debug enabled");
    }

    private final CanonicalRegistry registry;
    private final MidiIOManager ioManager;

    public OutputRouter(CanonicalRegistry registry, MidiIOManager ioManager) {
        this.registry = registry;
        this.ioManager = ioManager;
    }

    /**
     * Apply a change to hardware using the currently selected transport mode.
     */
    public void applyChange(String canonicalId, int newValue) {
        if (DEBUG) {
            logger.info("OutputRouter.applyChange: " + canonicalId + " = " + newValue);
        }

        ControlInstance ci = registry.resolveCanonicalId(canonicalId);
        if (ci == null) {
            logger.warning("Unknown canonicalId: " + canonicalId);
            return;
        }

        TransportMode mode = ioManager.getTransportMode();

        switch (mode) {

            case NRPN:
                if (ci.getNrpn().isPresent()) {
                    sendNrpn(ci, newValue);
                } else {
                    sendSysex(ci, newValue); // fallback
                }
                break;
            case SYSEX:
            default:
                sendSysex(ci, newValue);
                break;
        }
    }

    /**
     * Request a value from hardware (always SYSEX for Yamaha desks).
     */
    public void applyRequest(String canonicalId) {
        if (DEBUG) {
            logger.info("OutputRouter.applyRequest: " + canonicalId);
        }

        ControlInstance ci = registry.resolveCanonicalId(canonicalId);
        if (ci == null) {
            throw new IllegalArgumentException("Unknown canonicalId: " + canonicalId);
        }

        byte[] msg = ci.getSysex().buildRequestMessage(ci.getIndex());

        if (DEBUG) {
            logger.info("Sending SYSEX REQUEST OUT: " + bytesToHex(msg));
        }

        ioManager.sendAsync(msg);
    }

    // -------------------------------------------------------------------------
    // Transport-specific send helpers
    // -------------------------------------------------------------------------

    private void sendNrpn(ControlInstance ci, int newValue) {
        List<byte[]> msgs = ci.getNrpn().get().buildNrpnBytes(Optional.of(ci), newValue);

        if (DEBUG) {
            logger.info("NRPN mapping used for " + ci.getCanonicalId());
            for (byte[] msg : msgs) {
                logger.info("Sending NRPN OUT: " + bytesToHex(msg));
            }
        }

        for (byte[] msg : msgs) {
            ioManager.sendAsync(msg);
        }
    }

    private void sendSysex(ControlInstance ci, int newValue) {
        byte[] msg = ci.getSysex().buildChangeMessage(newValue, ci.getIndex());

        if (DEBUG) {
            logger.info("SYSEX mapping used for " + ci.getCanonicalId());
            logger.info("Sending SYSEX OUT: " + bytesToHex(msg));
        }

        ioManager.sendAsync(msg);
    }
    // -------------------------------------------------------------------------

    private static String bytesToHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}