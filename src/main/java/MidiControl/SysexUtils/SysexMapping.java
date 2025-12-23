package MidiControl.SysexUtils;

import java.util.ArrayList;
import java.util.List;

public class SysexMapping {

    private String control_group;
    private int control_id;
    private int max_channels;
    private String sub_control;
    private int channel_index;
    private long key;
    private int value;
    private int min_value;
    private int max_value;
    private int default_value;
    private String comment;

    private List<String> parameter_change_format;
    private List<String> parameter_request_format;

    private int[] valueByteIndices;
    private int[] indexByteIndices;

    // TODO: make this configurable from outside if needed
    private static int deviceNumber = 0; // 0–15, default 0

    public SysexMapping(
            String control_group,
            int control_id,
            int max_channels,
            String sub_control,
            int channel_index,
            long key,
            int value,
            int min_value,
            int max_value,
            int default_value,
            String comment,
            List<String> parameter_change_format,
            List<String> parameter_request_format
    ) {
        this.control_group = control_group;
        this.control_id = control_id;
        this.max_channels = max_channels;
        this.sub_control = sub_control;
        this.channel_index = channel_index;
        this.key = key;
        this.value = value;
        this.min_value = min_value;
        this.max_value = max_value;
        this.default_value = default_value;
        this.comment = comment;
        this.parameter_change_format = parameter_change_format;
        this.parameter_request_format = parameter_request_format;

        computeValueByteIndices();
        computeIndexByteIndices();
    }

    public void initialize() {
        computeValueByteIndices();
        computeIndexByteIndices();
    }

    private void computeValueByteIndices() {
        if (parameter_change_format == null) {
            valueByteIndices = new int[0];
            return;
        }

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < parameter_change_format.size(); i++) {
            if ("dd".equals(parameter_change_format.get(i))) {
                indices.add(i);
            }
        }
        valueByteIndices = indices.stream().mapToInt(Integer::intValue).toArray();
    }

    private void computeIndexByteIndices() {
        if (parameter_change_format == null) {
            indexByteIndices = new int[0];
            return;
        }

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < parameter_change_format.size(); i++) {
            if ("cc".equals(parameter_change_format.get(i))) {
                indices.add(i);
            }
        }

        indexByteIndices = indices.stream().mapToInt(Integer::intValue).toArray();
    }

    public int[] getValueByteIndices() { return valueByteIndices; }
    public int[] getIndexByteIndices() { return indexByteIndices; }

    private Byte parseStatusToken(Object token) {
        if (!(token instanceof String s)) return null;

        // Yamaha "1n" / "3n" tokens – high nibble is 1 or 3, low nibble is device number
        if ("1n".equalsIgnoreCase(s)) {
            return (byte) ((0x10) | (deviceNumber & 0x0F));
        }
        if ("3n".equalsIgnoreCase(s)) {
            return (byte) ((0x30) | (deviceNumber & 0x0F));
        }

        return null;
    }

    /**
     * Parses a literal SysEx byte token from the mapping.
     *
     * Supports:
     *   - Numeric JSON literals (e.g., 240, 67, 62, 28) → decimal
     *   - "0xNN" → hex literal
     *   - "1n"   → high nibble 0x10 OR'd with device number
     *   - "3n"   → high nibble 0x30 OR'd with device number
     *
     * Returns null for non-literal tokens ("cc", "dd").
     *
     * IMPORTANT:
     *   Literal bytes MUST NOT be masked with & 0x7F.
     *   Only cc/dd bytes are 7-bit.
     */
    private Byte parseLiteralByte(Object token) {
        // JSON numeric literal (e.g., 240, 67, 62, 28)
        if (token instanceof Number n) {
            return (byte) n.intValue();   // FULL 8-BIT VALUE
        }

        if (!(token instanceof String s)) {
            return null;
        }

        // Skip placeholders
        if (s.equals("cc") || s.equals("dd")) {
            return null;
        }

        // Device nibble patterns
        if (s.equalsIgnoreCase("1n")) {
            return (byte) (0x10 | (deviceNumber & 0x0F));
        }
        if (s.equalsIgnoreCase("3n")) {
            return (byte) (0x30 | (deviceNumber & 0x0F));
        }

        // Hex literal: "0xNN"
        if (s.startsWith("0x") || s.startsWith("0X")) {
            return (byte) Integer.parseInt(s.substring(2), 16);
        }

        // Decimal literal: "NN"
        try {
            return (byte) Integer.parseInt(s);   // FULL 8-BIT VALUE
        } catch (NumberFormatException ex) {
            return null;
        }
    }


    public int extractValue(byte[] sysex) {
        // Yamaha 01V96i faders: last two bytes of the 4-byte data block
        if (valueByteIndices.length == 4) {
            int b2 = sysex[valueByteIndices[valueByteIndices.length-2]] & 0x7F;
            int b3 = sysex[valueByteIndices[valueByteIndices.length-1]] & 0x7F;

            // 14-bit Yamaha fader value (matches NRPN)
            return (b2 << 7) | b3;
        }

        // Fallback: original 7-bit chunk logic
        int value = 0;
        int shift = 0;

        for (int idx : valueByteIndices) {
            int b = sysex[idx] & 0x7F;
            value |= (b << shift);
            shift += 7;
        }

        return value;
    }


    public int extractIndex(byte[] sysex) {
        int value = 0;
        int shift = 0;

        for (int idx : indexByteIndices) {
            int b = sysex[idx] & 0x7F;
            value |= (b << shift);
            shift += 7;
        }
        return value;
    }

    public byte[] buildChangeMessage(int value, int index) {

        List<Byte> out = new ArrayList<>();

        int[] valueChunks = splitInto7BitChunks(value, valueByteIndices.length);
        int[] indexChunks = splitInto7BitChunks(index, indexByteIndices.length);

        int valueChunkPos = 0;
        int indexChunkPos = 0;

        for (Object token : parameter_change_format) {

            // dd = value bytes
            if ("dd".equals(token)) {
                out.add((byte) (valueChunks[valueChunkPos++] & 0x7F));
                continue;
            }

            // cc = index bytes
            if ("cc".equals(token)) {
                out.add((byte) (indexChunks[indexChunkPos++] & 0x7F));
                continue;
            }

            // Literal/status byte (hex, decimal, "1n", "3n", or numeric)
            Byte literal = parseLiteralByte(token);
            if (literal != null) {
                out.add(literal);
                continue;
            }

            System.out.println("DEBUG: Unknown token in change format");
            System.out.println("  token class = " + token.getClass().getName());
            System.out.println("  token value = " + token);
            System.out.println("  full format = " + parameter_change_format);

            throw new IllegalArgumentException("Unknown SysEx token in change format: " + token);
        }

        return toByteArray(out);
    }

    public byte[] buildRequestMessage(int index) {

        List<Byte> out = new ArrayList<>();

        int[] indexChunks = splitInto7BitChunks(index, indexByteIndices.length);
        int indexChunkPos = 0;

        for (Object token : parameter_request_format) {

            // cc = index bytes
            if ("cc".equals(token)) {
                out.add((byte) (indexChunks[indexChunkPos++] & 0x7F));
                continue;
            }

            // dd = literal data bytes (requests do NOT carry a value payload)
            if ("dd".equals(token)) {
                out.add((byte) 0x00);
                continue;
            }

            // Literal/status byte (hex, decimal, "1n", "3n", or numeric)
            Byte literal = parseLiteralByte(token);
            if (literal != null) {
                out.add(literal);
                continue;
            }

            throw new IllegalArgumentException("Unknown SysEx token in request format: " + token);
        }

        return toByteArray(out);
    }

    private int[] splitInto7BitChunks(int value, int count) {
        int[] chunks = new int[count];
        for (int i = 0; i < count; i++) {
            int shift = 7 * (count - 1 - i);  // MSB-first
            chunks[i] = (value >> shift) & 0x7F;
        }
        return chunks;
    }


    private byte[] toByteArray(List<Byte> list) {
        byte[] arr = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    public String getControlGroup() { return control_group; }
    public int getControl_id() { return control_id; }
    public int getMax_Channels() { return max_channels; }
    public String getSubControl() { return sub_control; }
    public int getChannel_index() { return channel_index; }
    public long getKey() { return key; }
    public int getValue() { return value; }
    public int getMin_value() { return min_value; }
    public int getMax_value() { return max_value; }
    public int getDefault_value() { return default_value; }
    public String getComment() { return comment; }
    public List<String> getParameter_change_format() { return parameter_change_format; }
    public List<String> getParameter_request_format() { return parameter_request_format; }

    // Optionally expose device number control later
    public static void setDeviceNumber(int n) {
        deviceNumber = n & 0x0F;
    }
}