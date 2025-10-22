package MidiControl.Utilities;

public interface NRPNDispatchTarget {
    void handleResolvedNRPN(NrpnMapping mapping, int value);
}

