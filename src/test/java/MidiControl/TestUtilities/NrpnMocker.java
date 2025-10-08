package MidiControl.TestUtilities;
import MidiControl.Utilities.NrpnParser;
import javax.sound.midi.ShortMessage;

public class NrpnMocker {
    private final NrpnParser parser;

    public NrpnMocker(NrpnParser parser) {
        this.parser = parser;
    }

    public void sendNrpn(int msb, int lsb, int value) throws Exception {
        int valueMsb = (value >> 7) & 0x7F;
        int valueLsb = value & 0x7F;

        parser.parse(new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 99, msb));
        parser.parse(new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 98, lsb));
        parser.parse(new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 6, valueMsb));
        parser.parse(new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 38, valueLsb));
    }
}

