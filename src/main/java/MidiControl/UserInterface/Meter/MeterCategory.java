package MidiControl.UserInterface.Meter;

public enum MeterCategory {
    INPUT(0), BUS(1), AUX(2), STEREO(4), MONITOR(5), EFFECT(6),
    SIGMA_CLIP(0x10), DISABLE(127), UNKNOWN(-1);

    private final int code;
    MeterCategory(int code) { this.code = code; }
    public int getCode() { return code; }
    public static MeterCategory fromInt(int i) {
        for (MeterCategory c : values()) if (c.code == i) return c;
        return UNKNOWN;
    }

    public static MeterCategory fromName(String name) {
        for (MeterCategory category : values()) {
            if (category.name().equalsIgnoreCase(name)) return category;
        }
        return UNKNOWN;
    }

    public static boolean isSupportedForModel(int modelByte, int category) {
        return switch (modelByte & 0xFF) {
            case 0x1A -> category == 0 || category == 1 || category == 2
                    || category == 4 || category == 5 || category == 6;
            case 0x11 -> category == 0 || category == 0x10;
            default -> false;
        };
    }
}
