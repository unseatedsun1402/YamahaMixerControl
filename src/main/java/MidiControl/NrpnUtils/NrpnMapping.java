package MidiControl.NrpnUtils;

import java.util.List;
import java.util.Optional;

import MidiControl.Controls.ControlInstance;

public class NrpnMapping {
    private static final int MIN = 0;
    private static final int MIDI_7BIT_MAX = 127;
    private static final int MIDI_10BIT_MAX = 1023;
    private static final int MIDI_14BIT_MAX = 16383;

    private final String canonical_id;
    private final String msb;
    private final String lsb;
    private final String value_mode;

    public NrpnMapping(String msb, String lsb, String canonical_id) {
        this(msb, lsb, canonical_id, null);
    }

    public NrpnMapping(String msb, String lsb, String canonical_id, String value_mode) {
        this.msb = msb;
        this.lsb = lsb;
        this.canonical_id = canonical_id;
        this.value_mode = value_mode;
    }

    public int msbInt() {
        return parse(this.msb);
    }

    public int lsbInt() {
        return parse(this.lsb);
    }

    private int parse(String s) {
        if (s == null) {
            return 0;
        }

        String trimmed = s.trim();

        if (trimmed.startsWith("0x") || trimmed.startsWith("0X")) {
            return Integer.parseInt(trimmed.substring(2), 16);
        }

        return Integer.parseInt(trimmed);
    }

    public List<byte[]> buildNrpnBytes(Optional<ControlInstance> ci, int value) {
        return buildNrpnBytes(value);
    }

    public List<byte[]> buildNrpnBytes(int value) {
        return switch (value_mode) {
            case "NRPN_14BIT" -> build2byte(value);
            case "NRPN_RAW_SPLIT" -> buildRawSplit(value);
            case "CC6_ONLY" -> buildCc6Only(value);
            default -> throw new IllegalStateException(
                "Unknown value_mode '" + value_mode + "' for " + canonical_id
            );
        };
    }

    private List<byte[]> buildCc6Only(int value) {
        int v = clamp(value, 0, 127);

        return List.of(
            cc(99, msbInt()),   // NRPN MSB
            cc(98, lsbInt()),   // NRPN LSB
            cc(6, v)            // Data Entry MSB
        );
    }

    private List<byte[]> build2byte(int value) {
        int raw = clamp(value, 0, 16383);

        return List.of(
            cc(99, msbInt()),
            cc(98, lsbInt()),
            cc(6, (raw >> 7) & 0x7F),   // MSB
            cc(38, raw & 0x7F)          // LSB
        );
    }

    private List<byte[]> buildRawSplit(int value) {
        int clamped = clamp(value, MIN, MIDI_14BIT_MAX);

        return List.of(
            cc(99, msbInt()),
            cc(98, lsbInt()),
            cc(6, (clamped >> 7) & 0x7F),
            cc(38, clamped & 0x7F)
        );
    }

    private byte[] cc(int controller, int value) {
        return new byte[] {
            (byte) 0xB0,
            (byte) clamp(controller, 0, 127),
            (byte) clamp(value, 0, 127)
        };
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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

    public int getMin(){
        switch (value_mode) {
            case "NRPN_14BIT":
                return MIN;
            case "CC6_ONLY":
                return MIN;
            default:
                return MIN;
        }
    }

    public int getMax(){
        switch (value_mode) {
            case "NRPN_14BIT":
                return MIDI_14BIT_MAX;
            case "CC6_ONLY":
                return MIDI_7BIT_MAX;
            default:
                return MIDI_10BIT_MAX;
        }
    }

    public String getValueMode() {
        return this.value_mode;
    }
}