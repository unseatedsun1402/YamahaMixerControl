package MidiControl.SysexUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class SysexMapping {

    private String control_group;
    private int control_id;
    private int max_channels;
    private String sub_control;
    private int channel_index;
    private long key;

    // NEW: authoritative metadata from JSON
    private int[] address_bytes;
    private int[] index_bytes;

    private int value;
    private int min_value;
    private int max_value;
    private int default_value;
    private String comment;

    private List<String> parameter_change_format;
    private List<String> parameter_request_format;

    // Derived from tokens
    private int[] valueByteIndices;
    private int[] indexByteIndices;

    public int priority;

    private static int deviceNumber = 0; // 0–15

    public SysexMapping(
            String control_group,
            int control_id,
            int max_channels,
            String sub_control,
            int channel_index,
            long key,
            int[] address_bytes,
            int[] index_bytes,
            int value,
            int min_value,
            int max_value,
            int default_value,
            String comment,
            List<String> parameter_change_format,
            List<String> parameter_request_format,
            int priority
    ) {
        this.control_group = control_group;
        this.control_id = control_id;
        this.max_channels = max_channels;
        this.sub_control = sub_control;
        this.channel_index = channel_index;
        this.key = key;
        this.address_bytes = address_bytes;
        this.index_bytes = index_bytes;
        this.value = value;
        this.min_value = min_value;
        this.max_value = max_value;
        this.default_value = default_value;
        this.comment = comment;
        this.parameter_change_format = parameter_change_format;
        this.parameter_request_format = parameter_request_format;
        this.priority = priority;

        initialize();
    }

    public SysexMapping() {
        // for JSON deserialization
    }

    public void initialize() {
        computeValueByteIndices();
        computeIndexByteIndicesFromMetadata();
    }

    /**
     * Value bytes still come from "dd" tokens.
     */
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

    /**
     * Index bytes now come EXCLUSIVELY from metadata.
     */
    private void computeIndexByteIndicesFromMetadata() {
        if (index_bytes != null && index_bytes.length > 0) {
            indexByteIndices = Arrays.copyOf(index_bytes, index_bytes.length);
        } else {
            indexByteIndices = new int[0];
        }
    }

    public int[] getValueByteIndices() { return valueByteIndices; }
    public int[] getIndexByteIndices() { return indexByteIndices; }

    private Byte parseLiteralByte(Object token) {
        if (token instanceof Number n) {
            return (byte) n.intValue();
        }

        if (!(token instanceof String s)) return null;

        if (s.equals("cc") || s.equals("dd")) return null;

        if (s.equalsIgnoreCase("1n")) return (byte) (0x10 | (deviceNumber & 0x0F));
        if (s.equalsIgnoreCase("3n")) return (byte) (0x30 | (deviceNumber & 0x0F));

        try {
            return (byte) Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public int extractValue(byte[] sysex) {
        int value = 0;

        for (int idx : valueByteIndices) {
            value = (value << 7) | (sysex[idx] & 0x7F);
        }

        return value;
    }

    public int extractIndex(byte[] sysex) {
        int value = 0;

        for (int idx : indexByteIndices) {
            value = (value << 7) | (sysex[idx] & 0x7F);
        }

        Logger.getLogger("SysexMapping").fine("Index extracted as "+value);

        return value;
    }

    public byte[] buildChangeMessage(int value, int index) {
        List<Byte> out = new ArrayList<>();

        int[] valueChunks = splitInto7BitChunks(value, valueByteIndices.length);
        int[] indexChunks = splitInto7BitChunks(index, indexByteIndices.length);

        int vPos = 0;
        int iPos = 0;

        for (Object token : parameter_change_format) {

            if ("dd".equals(token)) {
                out.add((byte) (valueChunks[vPos++] & 0x7F));
                continue;
            }

            if ("cc".equals(token)) {
                out.add((byte) (indexChunks[iPos++] & 0x7F));
                continue;
            }

            Byte literal = parseLiteralByte(token);
            if (literal != null) {
                out.add(literal);
                continue;
            }

            throw new IllegalArgumentException("Unknown SysEx token: " + token);
        }

        return toByteArray(out);
    }

    public byte[] buildRequestMessage(int index) {
        List<Byte> out = new ArrayList<>();

        int[] indexChunks = splitInto7BitChunks(index, indexByteIndices.length);
        int iPos = 0;

        for (Object token : parameter_request_format) {

            if ("cc".equals(token)) {
                out.add((byte) (indexChunks[iPos++] & 0x7F));
                continue;
            }

            if ("dd".equals(token)) {
                out.add((byte) 0x00);
                continue;
            }

            Byte literal = parseLiteralByte(token);
            if (literal != null) {
                out.add(literal);
                continue;
            }

            throw new IllegalArgumentException("Unknown SysEx token: " + token);
        }

        return toByteArray(out);
    }

    private int[] splitInto7BitChunks(int value, int count) {
        int[] chunks = new int[count];
        for (int i = 0; i < count; i++) {
            int shift = 7 * (count - 1 - i);
            chunks[i] = (value >> shift) & 0x7F;
        }
        return chunks;
    }

    private byte[] toByteArray(List<Byte> list) {
        byte[] arr = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    // Getters
    public String getControlGroup() { return control_group; }
    public int getControl_id() { return control_id; }
    public int getMax_Channels() { return max_channels; }
    public String getSubControl() { return sub_control; }
    public int getChannel_index() { return channel_index; }
    public long getKey() { return key; }
    public int[] getAddressBytes() { return address_bytes; }
    public int[] getIndexBytes() { return index_bytes;}
    public int getValue() { return value; }
    public int getMin_value() { return min_value; }
    public int getMax_value() { return max_value; }
    public int getDefault_value() { return default_value; }
    public String getComment() { return comment; }
    public List<String> getParameter_change_format() { return parameter_change_format; }
    public List<String> getParameter_request_format() { return parameter_request_format; }
}