package MidiControl.functional;
import MidiControl.NrpnUtils.NRPNDispatchTarget;
import MidiControl.NrpnUtils.NrpnMapping;
import MidiControl.NrpnUtils.NrpnParser;
import MidiControl.NrpnUtils.NrpnRegistry;

public class NrpnTestRunner {
    public static void main(String[] args) throws Exception {
        NrpnParser parser = new NrpnParser();
        MidiControl.TestUtilities.NrpnMocker mocker = new MidiControl.TestUtilities.NrpnMocker(parser);

        // Optional: set a dispatch target
        NrpnParser.setDispatchTarget(new NRPNDispatchTarget() {
            public void handleResolvedNRPN(NrpnMapping mapping, int value) {
                System.out.println("Dispatched: " + mapping.label() + " → " + value);
            }
        });

        // Load registry
        NrpnRegistry.getInstance().loadFromJson("nrpn_mappings.json");

        // Send a known NRPN
        mocker.sendNrpn(0x01, 0x00, 8192); // Mix 9 Send 3
    }
}