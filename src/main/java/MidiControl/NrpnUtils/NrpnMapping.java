package MidiControl.NrpnUtils;

import java.util.List;
import java.util.Optional;

import MidiControl.Controls.ControlInstance;

public class NrpnMapping {

    private static final int CANONICAL_MIN = 0;
    private static final int CANONICAL_MAX = 1023;
    private static final int MIDI_7BIT_MIN = 0;
    private static final int MIDI_7BIT_MAX = 127;
    private static final int MIDI_14BIT_MIN = 0;
    private static final int MIDI_14BIT_MAX = 16383;

    private final String canonical_id;
    private final String msb;
    private final String lsb;
    private final String value_mode;
    private final String nrpn_mode;

    public NrpnMapping(String msb, String lsb, String canonical_id) {
        this(msb, lsb, canonical_id, null);
    }

    public NrpnMapping(String msb, String lsb, String canonical_id, String value_mode) {
        this.msb = msb;
        this.lsb = lsb;
        this.canonical_id = canonical_id;
        this.value_mode = value_mode;
        this.nrpn_mode = null;
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
        return switch (mode()) {
            case "NRPN_14BIT" -> build14bit(value);
            case "NRPN_RAW_SPLIT" -> buildRawSplit(value);
            case "CC6_ONLY" -> buildCc6Only(value);
            default -> buildCc6Only(value);
        };
    }

    private String mode() {
        String selected = firstNonBlank(value_mode, nrpn_mode);

        if (selected == null) {
            return "CC6_ONLY";
        }

        return selected.trim().toUpperCase();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }

        if (second != null && !second.isBlank()) {
            return second;
        }

        return null;
    }

    private List<byte[]> buildCc6Only(int value) {
        int v = scale(value, CANONICAL_MIN, CANONICAL_MAX, MIDI_7BIT_MIN, MIDI_7BIT_MAX);

        return List.of(
            cc(99, msbInt()),
            cc(98, lsbInt()),
            cc(6, v)
        );
    }

    private List<byte[]> build14bit(int value) {
        int scaled = scale(value, CANONICAL_MIN, CANONICAL_MAX, MIDI_14BIT_MIN, MIDI_14BIT_MAX);

        return List.of(
            cc(99, msbInt()),
            cc(98, lsbInt()),
            cc(6, (scaled >> 7) & 0x7F),
            cc(38, scaled & 0x7F)
        );
    }

    private List<byte[]> buildRawSplit(int value) {
        int clamped = clamp(value, MIDI_14BIT_MIN, MIDI_14BIT_MAX);

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

    private int scale(int value, int inMin, int inMax, int outMin, int outMax) {
        if (value <= inMin) {
            return outMin;
        }

        if (value >= inMax) {
            return outMax;
        }

        double normalised = (value - inMin) / (double) (inMax - inMin);

        return (int) Math.round(outMin + normalised * (outMax - outMin));
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

    public String getValueMode() {
        return this.value_mode;
    }

    public String getNrpnMode() {
        return this.nrpn_mode;
    }

    public String getEffectiveMode() {
        return mode();
    }
}