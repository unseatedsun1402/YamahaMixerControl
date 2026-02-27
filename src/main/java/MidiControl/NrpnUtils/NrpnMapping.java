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

            case (byte) 0xFF:
            default:
                return buildBits(value);
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
     * 2 byte NRPN (value 0–1023)
     * MSB = remaining bits
     * LSB = bottom y bits shifted into Yamaha's nibble format
     */
    private List<byte[]> buildBits(int value) {
        int msbVal = (value >> 7) & 0x7F;   // top 7 bits
        int lsbVal = value & 0x7F;          // bottom 7 bits

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