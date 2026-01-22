package MidiControl.UserInterface.Meter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builds a SysEx meter request:
 * F0 43 3n DIGITAL_MIXER model 21 category source start countH countL [offsets...] F7
 *
 * - request byte is 0x30 | (channel & 0x0F)
 * - DIGITAL_MIXER (0x3E) is emitted before model
 * - startChannel: 0..126 = channel index, 127 = all channels
 * - channelCount: 1..0x3FFF (14-bit) encoded as two 7-bit bytes (high, low)
 * - offsets: optional tail bytes appended after count bytes (single offsets or vector pairs)
 *
 * All payload bytes (except F0 and F7) are validated as 7-bit (0..127).
 */
public final class MeterRequest {

    private static final byte SYSEX_START = (byte) 0xF0;
    private static final byte SYSEX_END   = (byte) 0xF7;
    private static final byte VENDOR_YAMAHA = 0x43;
    private static final byte METER_TYPE = 0x21;
    private static final byte DIGITAL_MIXER = 0x3E;

    private final int channel;           // 0..15 MIDI channel (encoded into 0x30..0x3F)
    private final byte model;            // 0..127 (do not use 0xF7)
    private final byte category;         // 0..127
    private final byte source;           // 0..127

    // start channel and channel count (count is 14-bit)
    private int startChannel = 0;        // 0..127 (127 == all channels)
    private int channelCount = 1;        // 1..0x3FFF

    // dynamic tail: single offsets or vector pairs appended after count bytes
    private final List<Byte> offsets;

    public MeterRequest(int channel, int model, int category, int source) {
        if (channel < 0 || channel > 15) throw new IllegalArgumentException("channel must be 0..15");
        this.channel = channel;
        this.model = validate7bit(model, "model");
        this.category = validate7bit(category, "category");
        this.source = validate7bit(source, "source");
        this.offsets = new ArrayList<>();
    }

    public MeterRequest(int channel, byte category, byte source) {
        this(channel, (byte) 0x00, category, source);
    }

    /**
     * Set the start channel (0..127). 127 means "all channels".
     */
    public MeterRequest setStartChannel(int start) {
        if (start < 0 || start > 127) throw new IllegalArgumentException("startChannel must be 0..127");
        this.startChannel = start;
        return this;
    }

    /**
     * Set the channel count (1..0x3FFF). This is encoded as two 7-bit bytes.
     */
    public MeterRequest setChannelCount(int count) {
        if (count < 0 || count > 0x3FFF) throw new IllegalArgumentException("channelCount must be 0..16383 (14-bit)");
        this.channelCount = count;
        return this;
    }

    /**
     * Add a vector pair (start,end). Hardware will interpolate between them.
     * Each value must be 0..127.
     */
    public MeterRequest addVector(int vecStart, int vecEnd) {
        this.offsets.add(validate7bit(vecStart, "offset"));
        this.offsets.add(validate7bit(vecEnd, "offset"));
        return this;
    }

    /**
     * Add explicit single offsets (0..127 each).
     */
    public MeterRequest addOffsets(int... offs) {
        for (int o : offs) {
            this.offsets.add(validate7bit(o, "offset"));
        }
        return this;
    }

    /**
     * Add explicit single offsets from a byte array.
     */
    public MeterRequest addOffsets(byte[] offs) {
        for (byte b : offs) {
            this.offsets.add(validate7bit(b & 0xFF, "offset"));
        }
        return this;
    }

    /**
     * Build the SysEx byte array.
     * Structure:
     * F0, 43, (0x30|channel), DIGITAL_MIXER, model, 21, category, source,
     * startChannel, countH, countL, [offsets...], F7
     */
    public byte[] toByteArray() {
        // base bytes before offsets: 11 bytes (indices 0..10), terminator adds 1 -> 12
        int len = 12; // 12 = header+fields+count bytes+terminator
        byte[] out = new byte[len];
        int i = 0;
        out[i++] = SYSEX_START;
        out[i++] = VENDOR_YAMAHA;
        out[i++] = (byte) (0x30 | (channel & 0x0F)); // request 0x30..0x3F
        out[i++] = DIGITAL_MIXER;
        out[i++] = model;
        out[i++] = METER_TYPE;
        out[i++] = category;
        out[i++] = source;
        out[i++] = (byte) (startChannel & 0x7F);
        int countH = (channelCount >> 7) & 0x7F;
        int countL = channelCount & 0x7F;
        out[i++] = (byte)countH;
        out[i++] = (byte)countL;
        out[i++] = SYSEX_END;

        return out;
    }

    /**
     * Hex string for logging (uppercase, space separated).
     */
    public String toHexString() {
        byte[] arr = toByteArray();
        StringBuilder sb = new StringBuilder(arr.length * 3);
        for (int j = 0; j < arr.length; j++) {
            sb.append(String.format("%02X", arr[j]));
            if (j + 1 < arr.length) sb.append(' ');
        }
        return sb.toString();
    }

    private static byte validate7bit(int v, String name) {
        if (v < 0 || v > 0x7F) throw new IllegalArgumentException(name + " must be 0..127 (7-bit)");
        return (byte) v;
    }

    @Override
    public String toString() {
        return "MeterRequest{" +
                "channel=" + channel +
                ", model=" + (model & 0xFF) +
                ", category=" + (category & 0xFF) +
                ", source=" + (source & 0xFF) +
                ", startChannel=" + startChannel +
                ", channelCount=" + channelCount +
                ", offsets=" + Arrays.toString(offsets.toArray()) +
                '}';
    }
}