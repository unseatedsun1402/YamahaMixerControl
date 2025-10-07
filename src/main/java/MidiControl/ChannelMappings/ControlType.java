package MidiControl.ChannelMappings;

public enum ControlType {
    FADER(0x10),
    PAN(0x20),
    MUTE(0x00),
    EFFECT_SEND(0x30),
    DIRECT_OUT(0x40),
    USER_DEFINED(0x50),
    REMOTE(0x60),
    SLOT_ASSIGN(0x70),
    MACHINE_CONFIG(0x80),
    GPI(0x90),
    EQ(0xA0),
    MATRIX_SEND(0xB0),
    SCENE_RECALL(0xC0),
    SCENE_STORE(0xD0),
    INPUT_SEND(0xE0);

    private final int controlByte;

    ControlType(int controlByte) {
        this.controlByte = controlByte;
    }

    public int getControlByte() {
        return controlByte;
    }
}
