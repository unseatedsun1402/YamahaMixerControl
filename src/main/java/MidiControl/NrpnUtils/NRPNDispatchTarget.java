package MidiControl.NrpnUtils;

public interface NRPNDispatchTarget {
    void handleResolvedNRPN(NrpnMapping mapping, int value);
}

