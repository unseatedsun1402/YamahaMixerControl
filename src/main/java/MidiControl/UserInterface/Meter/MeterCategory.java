package MidiControl.UserInterface.Meter;

public enum MeterCategory {
    INPUT(0), MIX(1), MATRIX(2), STEREO(3), MONITOR(4), EFFECT(5), DISABLE(127), UNKNOWN(-1);

    private final int code;
    MeterCategory(int code) { this.code = code; }
    public int getCode() { return code; }
    public static MeterCategory fromInt(int i) {
        for (MeterCategory c : values()) if (c.code == i) return c;
        return UNKNOWN;
    }
}
