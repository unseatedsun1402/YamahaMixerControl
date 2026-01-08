package MidiControl.NrpnUtils;

import java.util.List;
import java.util.Optional;

import MidiControl.Controls.ControlInstance;

public class NrpnMapping {

    private final String canonical_id;
    private final String msb;
    private final String lsb;

    public NrpnMapping(String msb, String lsb, String canonical_id) {
        this.msb = msb;
        this.lsb = lsb;
        this.canonical_id = canonical_id;
    }

    public int msbInt() { return parse(this.msb); }
    public int lsbInt() { return parse(this.lsb); }

    private int parse(String s) {
        if (s.startsWith("0x") || s.startsWith("0X"))
            return Integer.parseInt(s.substring(2), 16);
        return Integer.parseInt(s);
    }

    /**
     * Main NRPN builder — selects correct encoding based on resolution.
     */
    public List<byte[]> buildNrpnBytes(Optional<ControlInstance> ci, int value) {
        byte res = 0x0F;
        if (ci.isPresent()){
            res = ci.get().getResolution();
        }

        switch (res) {
            case (byte) 0x0F:  // 7-bit
                return build7bit(value);

            case (byte) 0xF0:  // 10-bit
                return build10bit(value);

            case (byte) 0xFF:  // 14-bit
            default:
                return build14bit(value);
        }
    }

    /**
     * 7-bit NRPN (value 0–127)
     * Only Data Entry MSB is used.
     */
    private List<byte[]> build7bit(int value) {
        int msbVal = value & 0x7F;

        return List.of(
            new byte[]{ (byte)0xB0, 99, (byte) msbInt() },   // NRPN MSB
            new byte[]{ (byte)0xB0, 98, (byte) lsbInt() },   // NRPN LSB
            new byte[]{ (byte)0xB0, 6,  (byte) msbVal }      // Data Entry MSB only
        );
    }

    /**
     * 10-bit NRPN (value 0–1023)
     * MSB = top 7 bits
     * LSB = bottom 3 bits shifted into Yamaha's nibble format
     */
    private List<byte[]> build10bit(int value) {
        int msbVal = (value >> 3) & 0x7F;     // top 7 bits
        int lsbVal = (value & 0x07) << 4;     // bottom 3 bits shifted left

        return List.of(
            new byte[]{ (byte)0xB0, 99, (byte) msbInt() },
            new byte[]{ (byte)0xB0, 98, (byte) lsbInt() },
            new byte[]{ (byte)0xB0, 6,  (byte) msbVal },
            new byte[]{ (byte)0xB0, 38, (byte) lsbVal }
        );
    }

    /**
     * 14-bit NRPN (value 0–16383)
     * MSB = top 7 bits
     * LSB = bottom 7 bits
     */
    private List<byte[]> build14bit(int value) {
        int msbVal = (value >> 7) & 0x7F;
        int lsbVal = value & 0x7F;

        return List.of(
            new byte[]{ (byte)0xB0, 99, (byte) msbInt() },
            new byte[]{ (byte)0xB0, 98, (byte) lsbInt() },
            new byte[]{ (byte)0xB0, 6,  (byte) msbVal },
            new byte[]{ (byte)0xB0, 38, (byte) lsbVal }
        );
    }

    public String getCanonicalId() {
        return this.canonical_id;
    }

    public String getMsb() {
        return this.msb;
    }

    public String getLsb() {
        return this.lsb;
    }
}