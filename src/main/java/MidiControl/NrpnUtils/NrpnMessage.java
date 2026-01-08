package MidiControl.NrpnUtils;

public class NrpnMessage {
    public final int msb;
    public final int lsb;
    public final int value;

    public NrpnMessage(int msb, int lsb, int value){
        this.msb = msb;
        this.lsb = lsb;
        this.value = value;
    }
}
