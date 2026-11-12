package MidiControl.UserInterface.Meter;

public enum MeterSource {
    CHANNEL(0), EQ_OUT(1), COMP_OUT(2), COMP_GR(3),
    GATE_OUT(4), GATE_GR(5), ALL(127),
    IN_PRE_HPF(0), IN_PRE_ATT(1), IN_PRE_EQ(2), IN_POST_DYN1(3),
    IN_POST_DYN2(4), IN_POST_ON(5), IN_PRE_FADER(6), IN_DYN1_GR(7),
    IN_DYN2_GR(8), ALL_POINT(127),
    UNKNOWN(-1);

    private final int code;
    MeterSource(int code) { this.code = code; }
    public int getCode() { return code; }
    public static MeterSource fromInt(int i) {
        for (MeterSource s : values()) if (s.code == i) return s;
        return UNKNOWN;
    }

    public static MeterSource fromInt(int i, int responseModelByte) {
        if ((responseModelByte & 0xFF) == 0x0F) {
            return switch (i) {
                case 0 -> IN_PRE_HPF;
                case 1 -> IN_PRE_ATT;
                case 2 -> IN_PRE_EQ;
                case 3 -> IN_POST_DYN1;
                case 4 -> IN_POST_DYN2;
                case 5 -> IN_POST_ON;
                case 6 -> IN_PRE_FADER;
                case 7 -> IN_DYN1_GR;
                case 8 -> IN_DYN2_GR;
                case 127 -> ALL_POINT;
                default -> UNKNOWN;
            };
        }
        return fromInt(i);
    }

    public static boolean isSupportedForRequest(int modelByte, int category, int source) {
        if ((modelByte & 0xFF) == 0x11) {
            return category == 0
                    ? source >= 0 && source <= 8 || source == 127
                    : category == 0x10 && (source == 0 || source == 127);
        }
        if ((modelByte & 0xFF) != 0x1A) {
            return false;
        }
        return switch (category) {
            case 0 -> source >= 0 && source <= 5 || source == 127;
            case 1, 2, 4 -> source >= 0 && source <= 3 || source == 127;
            case 5 -> source == 0;
            case 6 -> source >= 0 && source <= 19 || source == 127;
            default -> false;
        };
    }

    public static MeterSource fromName(String name) {
        for (MeterSource source : values()) {
            if (source.name().equalsIgnoreCase(name)) return source;
        }
        return UNKNOWN;
    }
}