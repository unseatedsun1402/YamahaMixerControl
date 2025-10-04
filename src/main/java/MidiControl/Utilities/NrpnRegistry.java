package MidiControl.Utilities;
import MidiControl.ChannelMappings.ChannelType;
import MidiControl.ChannelMappings.ControlType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NrpnRegistry {
    private static final NrpnRegistry INSTANCE = new NrpnRegistry();
    private final List<NrpnMapping> mappings = new ArrayList<>();

    public NrpnRegistry() {

        // Dynamically generate mappings
       for (int i = 0; i < 56; i++) {
            int nrpn = 0x0000 + i; // INPUT FADER range
            int msb = nrpn / 128;
            int lsb = nrpn % 128;
            mappings.add(new NrpnMapping(msb, lsb, ControlType.FADER, ChannelType.INPUT, i, "Input " + (i + 1) + " Fader"));
            Logger.getLogger(NrpnRegistry.class.getName()).log(Level.FINE, "Registered NRPN Mapping: MSB {0}, LSB {1} for {2}", new Object[]{msb, lsb, "Input " + (i + 1) + " Fader"});
        }
        for (int i = 0; i < 16; i++) {
            int nrpn = 0x060 + i; // MIX FADER range
            int msb = nrpn / 128;
            int lsb = nrpn % 128;
            mappings.add(new NrpnMapping(msb, lsb, ControlType.FADER, ChannelType.OUTPUT, i, "Mix " + (i + 1) + " Fader"));
        }
        for (int i = 0; i < 8; i++) {
            int nrpn = 0x070 + i; // MATRIX FADER range
            int msb = nrpn / 128;
            int lsb = nrpn % 128;
            mappings.add(new NrpnMapping(msb, lsb, ControlType.FADER, ChannelType.OUTPUT, i, "Matrix " + (i + 1) + " Fader"));
        }
        for (int i = 0; i < 5; i++) {
            int nrpn = 0x079 + i; // ST FADER range
            int msb = nrpn / 128;
            int lsb = nrpn % 128;
            mappings.add(new NrpnMapping(msb, lsb, ControlType.FADER, ChannelType.OUTPUT, i, "ST Bus " + (i + 1) + " Fader"));
        }
    }

    public static NrpnRegistry getInstance() {
        return INSTANCE;
    }

    public static Optional<NrpnMapping> resolve(int msb, int lsb) {
        Optional<NrpnMapping> result = INSTANCE.mappings.stream()
            .filter(m -> m.msb() == msb && m.lsb() == lsb)
            .findFirst();

        if (result.isEmpty()) {
            System.out.printf("Registry miss: MSB=0x%02X, LSB=0x%02X%n", msb, lsb);
        }

        return result;
    }
    public static void dump() {
        INSTANCE.mappings.forEach(m ->
            System.out.printf("MSB=0x%02X, LSB=0x%02X → %s%n", m.msb(), m.lsb(), m.label()));
    }
}
