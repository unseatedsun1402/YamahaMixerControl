package MidiControl.SystemTools;

public class Format {
    public static String bytesToHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) sb.append(String.format("%02X  ", b));
        return sb.toString();
    }
}
