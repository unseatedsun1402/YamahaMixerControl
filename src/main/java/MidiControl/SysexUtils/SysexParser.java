package MidiControl.SysexUtils;

import java.util.List;
import java.util.logging.Logger;

public class SysexParser {

    private static final Logger logger = Logger.getLogger(SysexParser.class.getName());
    private final SysexRegistry registry;

    public SysexParser(List<SysexMapping> mappings) {
        this.registry = new SysexRegistry(mappings);
    }

    public SysexMapping processMidiMessage(byte[] message) {

        // --- 1. Try native fast resolver first ---
        SysexMapping fast = registry.resolveFast(message);

        if (fast != null) {
            // logger.fine("[FAST] " + fast.getControlGroup() + " " + fast.getSubControl());
            return fast;
        }

        // --- 2. Fallback to Java resolver ---
        SysexMapping slow = registry.resolve(message);

        if (slow != null) {
            // logger.fine("[SLOW] " + slow.getControlGroup() + " " + slow.getSubControl());
            return slow;
        }

        // --- 3. No match ---
        logger.warning("Unrecognized Sysex message: " + bytesToHex(message));
        return null;
    }

    public static String bytesToHex(byte[] message) {
        StringBuilder sb = new StringBuilder();
        for (byte b : message) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}