package MidiControl.SysexUtils;

import java.util.logging.Logger;
import java.util.List;

public class SysexRegistry {
    private final List<SysexMapping> mappings;
    private final Logger log = Logger.getLogger(this.getClass().getName());
    public SysexRegistry(List<SysexMapping> mappings) {
        this.mappings = mappings;
    }

    public SysexMapping resolve(byte[] message) {
        // Length checks
        if (message.length < 7) {
            log.warning("Message too short: length=" + message.length);
            return null;
        }
        if (message.length < 18) {
            log.info("Message length " + message.length + " shorter than expected 18, skipping");
            return null;
        }
        if (message.length > 18) {
            log.info("Message length " + message.length + " longer than expected 18, skipping");
            return null;
        }
        for (SysexMapping mapping : mappings) {
            if (matchesFormat(message, mapping.getParameter_change_format()) ||
                matchesFormat(message, mapping.getParameter_request_format())) {
                return mapping;
            }
        }
        Logger.getLogger(SysexRegistry.class.getName())
            .info("No mapping matched for message: " + bytesToHex(message));
        return null;
    }


    private String bytesToHex(byte[] message) {
        StringBuilder sb = new StringBuilder();
        for (byte b : message) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    private boolean matchesFormat(byte[] message, List<Object> format) {
        int len = format.size();
        if (message.length < len) return false;

        // Build expected array with substitutions
        int[] expected_bytes = new int[len];
        boolean[] wildcard = new boolean[len];
        boolean[] nibble1n = new boolean[len];
        boolean[] nibble3n = new boolean[len];

        for (int i = 0; i < len; i++) {
            Object f = format.get(i);
            if (f instanceof Number) {
                expected_bytes[i] = ((Number) f).intValue();
            } else if ("1n".equals(f)) {
                nibble1n[i] = true;
            } else if ("3n".equals(f)) {
                nibble3n[i] = true;
            } else if ("dd".equals(f) || "cc".equals(f)) {
                wildcard[i] = true;
            } else {
                return false; // unknown token
            }
        }

        // Now compare arrays in one pass
        int[] actual_bytes = new int[len];
        for (int i = 0; i < len; i++) {
            actual_bytes[i] = message[i] & 0xFF;
        }
        for (int i = 0; i < len; i++) {
            if (wildcard[i]) continue;
            if (nibble1n[i] && (actual_bytes[i] & 0xF0) != 0x10) return false;
            else if (nibble3n[i] && (actual_bytes[i] & 0xF0) != 0x30) return false;
            else if (!nibble1n[i] && !nibble3n[i] && !wildcard[i]) {
                if (actual_bytes[i] != expected_bytes[i]) return false;
            }
        }

        // Require F7 terminator
        return (message[len - 1] & 0xFF) == 0xF7;
    }
}