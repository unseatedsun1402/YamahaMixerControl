package MidiControl.SysexUtils;

public enum ModelNumbers {
    YAMAHA_01V96I((byte)0x1A),
    YAMAHA_M7CL((byte)0x11);

    private final byte MODELBYTE;

    ModelNumbers(byte MODELBYTE) {
        this.MODELBYTE = MODELBYTE;
    }

    public byte getModelByte() {
        return MODELBYTE;
    }

    public static byte getModelByteByString(String key) {
        for (ModelNumbers mapping : ModelNumbers.values()) {
            if (mapping.name().equalsIgnoreCase(key)) {
                return mapping.getModelByte();
            }
        }
        return -1;
    }
}
