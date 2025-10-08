package MidiControl.unit;
import MidiControl.Utilities.NrpnMapping;
import MidiControl.Utilities.NRPNDispatchTarget;

public class TestDispatchTarget implements NRPNDispatchTarget {
    public NrpnMapping lastMapping;
    public int lastValue;

    @Override
    public void handleResolvedNRPN(NrpnMapping mapping, int value) {
        this.lastMapping = mapping;
        this.lastValue = value;
    }
}