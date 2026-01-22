package MidiControl.UserInterface.Meter;

public enum MeterSource {
    CHANNEL(0), EQ_OUT(1), COMP_OUT(2), COMP_GR(3),
    GATE_OUT(4), GATE_GR(5), ALL(127), UNKNOWN(-1);

    private final int code;
    MeterSource(int code) { this.code = code; }
    public int getCode() { return code; }
    public static MeterSource fromInt(int i) {
        for (MeterSource s : values()) if (s.code == i) return s;
        return UNKNOWN;
    }
}