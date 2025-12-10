public class Control {
    private final int controlId;
    private final SysexMapping sysexMapping;
    private final NrpnMapping nrpnMapping;
    private int value;

    public Control(int controlId, SysexMapping sysexMapping, NrpnMapping nrpnMapping) {
        this.controlId = controlId;
        this.sysexMapping = sysexMapping;
        this.nrpnMapping = nrpnMapping;
    }

    public boolean supportsSysex() { return sysexMapping != null; }
    public boolean supportsNrpn() { return nrpnMapping != null; }

    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }

    public SysexMapping getSysexMapping() { return sysexMapping; }
    public NrpnMapping getNrpnMapping() { return nrpnMapping; }
}
