package MidiControl.SysexUtils;

public enum ModelNumbers {
    YAMAHA_01V96I((byte)0x1A, (byte)0x1A),
    YAMAHA_M7CL((byte)0x11, (byte)0x11);

    private final byte MODELBYTE;
    private final byte meterModelByte;

    ModelNumbers(byte MODELBYTE, byte meterModelByte) {
        this.MODELBYTE = MODELBYTE;
        this.meterModelByte = meterModelByte;
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

    public byte getMeterModelByte() {
        return meterModelByte;
    }

    public static boolean isM7clMeterResponse(int modelByte) {
        return (modelByte & 0xFF) == (YAMAHA_M7CL.meterModelByte & 0xFF)
                || (modelByte & 0xFF) == 0x0F;
    }

    public static byte getMeterModelByteByString(String key) {
        for (ModelNumbers mapping : ModelNumbers.values()) {
            if (mapping.name().equalsIgnoreCase(key)) {
                return mapping.getMeterModelByte();
            }
        }
        return -1;
    }
}
