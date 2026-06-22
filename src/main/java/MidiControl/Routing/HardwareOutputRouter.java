package MidiControl.Routing;

import java.util.List;
import java.util.logging.Logger;

import MidiControl.Controls.ControlInstance;
import MidiControl.MidiDeviceManager.MidiIOManager;
import MidiControl.MidiDeviceManager.TransportMode;
import MidiControl.Controls.CanonicalRegistry;;

public class HardwareOutputRouter implements OutputRequestSender{

    private static final Logger logger = Logger.getLogger(HardwareOutputRouter.class.getName());
    private static boolean debug = false;

    public static void enableDebug() {
        debug = true;
        logger.info("OutputRouter debug enabled");
    }

    private final CanonicalRegistry registry;
    private final MidiIOManager ioManager;

    public HardwareOutputRouter(CanonicalRegistry registry, MidiIOManager ioManager) {
        this.registry = registry;
        this.ioManager = ioManager;
    }

    /**
     * Apply a change to hardware using the currently selected transport mode.
     */
    public void applyChange(String canonicalId, int newValue) {
        if (debug)logger.info("OutputRouter.applyChange: " + canonicalId + " = " + newValue);

        ControlInstance ci = registry.resolveCanonicalId(canonicalId);
        if (ci == null) {logger.warning("Unknown canonicalId: " + canonicalId);return;}

        TransportMode mode = ioManager.getTransportMode();

        switch (mode) {

            case NRPN:
                if (ci.getNrpn().isPresent()) sendNrpn(ci, newValue);

                else sendSysex(ci, newValue);

                break;
            case SYSEX:
                if (ci.getNrpn().isPresent()) {
                    sendNrpn(ci, newValue);
                    break;
                }
            default:
                sendSysex(ci, newValue);
        }
    }

    /**
     * Request a value from hardware (always SYSEX for Yamaha desks).
     */
    public void applyRequest(String canonicalId) {
        if (debug) {
            logger.info("OutputRouter.applyRequest: " + canonicalId);
        }

        ControlInstance ci = registry.resolveCanonicalId(canonicalId);
        if (ci == null) throw new IllegalArgumentException("Unknown canonicalId: " + canonicalId);

        byte[] msg = ci.getSysex().buildRequestMessage(ci.getIndex());

        if (debug) logger.fine(String.format("Sending SYSEX REQUEST OUT: %s",bytesToHex(msg)));
        ioManager.sendAsync(msg);
    }


    private void sendNrpn(ControlInstance ci, int newValue) {
        List<byte[]> msgs = ci.buildNrpnChange(newValue);

        for (byte[] msg : msgs) {
            if (debug) logger.fine(String.format("Sending %s NRPN mapping used for %s", bytesToHex(msg), ci.getCanonicalId()));
            ioManager.sendAsync(msg);
        }
    }

    private void sendSysex(ControlInstance ci, int newValue) {
        byte[] msg = ci.getSysex().buildChangeMessage(newValue, ci.getIndex());
        if (debug) logger.fine(String.format("Sending %s NRPN mapping used for %s", bytesToHex(msg), ci.getCanonicalId()));
        ioManager.sendAsync(msg);
    }

    private static String bytesToHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    @Override
    public void send(byte[] message) {
        this.ioManager.sendAsync(message);
    }
}