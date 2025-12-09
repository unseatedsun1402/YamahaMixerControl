package MidiControl.SysexUtils;

import java.util.*;
import java.util.logging.Logger;

public class SysexRegistry {
    private final List<SysexMapping> mappings;
    private final Map<Integer, List<SysexMapping>> byControlId = new HashMap<>();
    private final Logger log = Logger.getLogger(this.getClass().getName());
    private final int MATCH_LENGTH = 11;

    public SysexRegistry(List<SysexMapping> mappings) {
        this.mappings = mappings;

        // Build index by controlId (byte 8 of the format)
        for (SysexMapping m : mappings) {
            int controlId = m.getParameter_change_format().get(7) instanceof Number
                    ? ((Number) m.getParameter_change_format().get(7)).intValue()
                    : -1;
            byControlId.computeIfAbsent(controlId, k -> new ArrayList<>()).add(m);
        }
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

        // Extract controlId from incoming message (byte 8)
        int controlId = message[7] & 0xFF;
        List<SysexMapping> candidates = byControlId.getOrDefault(controlId, Collections.emptyList());

        // Only check candidates for this controlId
        for (SysexMapping mapping : candidates) {
            if (matchesFormat(message, mapping.getParameter_change_format())) {
                return mapping;
            }
        }

        log.info("No mapping matched for message: " + bytesToHex(message));
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

        int[] expected_bytes = new int[len];
        boolean[] wildcard = new boolean[len];
        boolean[] nibble = new boolean[len];

        for (int i = 0; i < len; i++) {
            Object f = format.get(i);
            if (f instanceof Number) {
                expected_bytes[i] = ((Number) f).intValue();
            } else if ("1n".equals(f)) {
                nibble[i] = true;
            } else if ("3n".equals(f)) {
                nibble[i] = true;
            } else if ("dd".equals(f) || "cc".equals(f)) {
                wildcard[i] = true;
            } else {
                return false; // unknown token
            }
        }

        int[] actual_bytes = new int[len];
        for (int i = 0; i < len; i++) {
            actual_bytes[i] = message[i] & 0xFF;
        }
        for (int i = 0; i < MATCH_LENGTH; i++) {
            if (wildcard[i]) continue;

            int actual = actual_bytes[i];
            int masked = actual & 0xF0;

            if (nibble[i]) {
                if (masked != 0x10 && masked != 0x30) return false;
                // if it matches, keep going
            } else {
                if (actual != expected_bytes[i]) return false;
            }
        }

        return (message[len - 1] & 0xFF) == 0xF7;
    }
}