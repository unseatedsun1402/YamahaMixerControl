package MidiControl.functional.Control;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;
import MidiControl.NrpnUtils.NrpnMapping;
import MidiControl.NrpnUtils.NrpnMappingLoader;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;

public class ControlInstanceTest {
    private List<SysexMapping> sysex_map = SysexMappingLoader.loadMappingsFromResource("MidiControl/m7cl_sysex_mappings.json");
    private List<NrpnMapping> nrpn_map = NrpnMappingLoader.loadFromResource("MidiControl/nrpn/m7cl_nrpn_mappings.json");

    private static final int MIDI_7BIT_MAX = 127;
    private static final int MIDI_10BIT_MAX = 1023;
    private static final int MIDI_14BIT_MAX = 16383;

    @Test
    void testToNRPNValueIncreases(){
        CanonicalRegistry testRegistry = new CanonicalRegistry(sysex_map, new SysexParser(sysex_map));
        testRegistry.attachNrpnMappings(nrpn_map);

        ControlInstance testMapping = testRegistry.getAllInstances().stream().filter(ci -> "kInputFader.kFader.0".equals(ci.getCanonicalId())).findFirst().get();

        int testVal = testMapping.toNrpnValue(MIDI_10BIT_MAX, 0, MIDI_14BIT_MAX);
        int expVal = MIDI_14BIT_MAX;
        assertEquals(expVal, testVal);
    }

    @Test
    void testToNRPNValueDecreases(){
        CanonicalRegistry testRegistry = new CanonicalRegistry(sysex_map, new SysexParser(sysex_map));
        testRegistry.attachNrpnMappings(nrpn_map);

        ControlInstance testMapping = testRegistry.getAllInstances().stream().filter(ci -> "kInputFader.kFader.0".equals(ci.getCanonicalId())).findFirst().get();

        int testVal = testMapping.toNrpnValue(MIDI_10BIT_MAX, 0, MIDI_7BIT_MAX);
        int expVal = MIDI_7BIT_MAX;
        assertEquals(expVal, testVal);
    }

    @Test
    void testToCanonicalValueScalesAutomatically(){
        CanonicalRegistry testRegistry = new CanonicalRegistry(sysex_map, new SysexParser(sysex_map));
        testRegistry.attachNrpnMappings(nrpn_map);

        ControlInstance testMapping = testRegistry.getAllInstances().stream().filter(ci -> "kInputFader.kFader.0".equals(ci.getCanonicalId())).findFirst().get();

        int testVal = testMapping.toCanonicalValue(MIDI_14BIT_MAX);
        int expVal = MIDI_10BIT_MAX;
        assertEquals(expVal, testVal);
    }
}
