package MidiControl.SysexUtils;

import java.util.logging.Logger;

public class SysexParser {
    private static final SysexRegistry registry =
        new SysexRegistry(SysexMappingLoader.loadAllMappings());

    public static String processIncomingMidiMessage(byte[] message) {
        SysexMapping mapping = registry.resolve(message);
        if (mapping != null) {
            int value = extractValue(message);
            Logger.getLogger(SysexParser.class.getName()).info(mapping.getControlGroup() + " (" + mapping.getSubControl() + ") = " +
                   interpretValue(mapping, value));
            return mapping.getControlGroup() + " (" + mapping.getSubControl() + ") = " +
                   interpretValue(mapping, value);
        }
        Logger.getLogger(SysexParser.class.getName())
              .warning("Unrecognized Sysex message: " + bytesToHex(message));
        return "Unknown Sysex message " + bytesToHex(message);
    }


    public static int extractValue(byte[] message) {
        return message[message.length - 2] & 0xFF;
    }

    private static String interpretValue(SysexMapping mapping, int value) {
        if (mapping.getComment() != null) {
            String[] parts = mapping.getComment().split(",");
            if (value < parts.length) {
                return parts[value].trim();
            }
        }
        return String.valueOf(value);
    }

    private static boolean isSysexMessage(byte[] message) {
        // Check if the message is a Sysex message
        return message != null && message.length >= 2 && message[0] == (byte) 0xF0 && message[message.length - 1] == (byte) 0xF7;
    }

    public static String bytesToHex(byte[] message) {
        StringBuilder sb = new StringBuilder();
        for (byte b : message) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
