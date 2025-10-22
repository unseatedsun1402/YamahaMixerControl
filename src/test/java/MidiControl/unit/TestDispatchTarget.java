package MidiControl.unit;
import MidiControl.NrpnUtils.NRPNDispatchTarget;
import MidiControl.NrpnUtils.NrpnMapping;

public class TestDispatchTarget implements NRPNDispatchTarget {
    public NrpnMapping lastMapping;
    public int lastValue;

    @Override
    public void handleResolvedNRPN(NrpnMapping mapping, int value) {
        this.lastMapping = mapping;
        this.lastValue = value;
    }
}