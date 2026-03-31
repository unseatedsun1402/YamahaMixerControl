package MidiControl.functional.CanonicalRegistry;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;
import MidiControl.NrpnUtils.NrpnMapping;
import MidiControl.NrpnUtils.NrpnMappingLoader;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;
public class ServerToHardwareTest {

    @Test
    public void testResolveAndBuildForFader1Sysex() throws Exception {
        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");
        List<NrpnMapping> nrpnMappings = NrpnMappingLoader.loadFromResource("MidiControl/nrpn/01v96i_nrpn_mappings.json");
        CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));
        registry.attachNrpnMappings(nrpnMappings);

        String canonicalId = "kInputFader.kFader.1";
        ControlInstance instance = registry.resolve(canonicalId);

        assertNotNull(instance, "Registry should resolve canonical ID: " + canonicalId);

        SysexMapping mapping = instance.getSysex();
        assertNotNull(mapping, "Instance should have a SysexMapping attached");

        int testValue = 770;
        byte[] built = mapping.buildChangeMessage(testValue, instance.getIndex());

        assertNotNull(built, "Builder should return a SysEx byte array");

        byte[] expected = {(byte)0xf0,0x43,0x10,0x3e,0x7f,0x01,0x1C,0x00,0x01,0x00,0x00,0x06,0x02,(byte) 0xf7};

        assertArrayEquals(expected, built,
            "Built SysEx must match mapping-defined SysEx format");
    }

    @Test
    public void testBuildNrpn2byte() throws Exception {
        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromResource("MidiControl/m7cl_sysex_mappings.json");
        List<NrpnMapping> nrpnMappings = NrpnMappingLoader.loadFromResource("MidiControl/nrpn/m7cl_nrpn_mappings.json");
        CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));
        registry.attachNrpnMappings(nrpnMappings);

        String canonicalId = "kInputFader.kFader.1";
        Optional<ControlInstance> instance = Optional.ofNullable(registry.resolve(canonicalId));

        assertNotNull(instance, "Registry should resolve canonical ID: " + canonicalId);

        NrpnMapping mapping = instance.get().getNrpn().get();
        assertNotNull(mapping, "Instance should have a SysexMapping attached");

        int testValue = 770;
        List<byte[]> built = mapping.buildNrpnBytes(instance,testValue);

        assertNotNull(built, "Builder should return a list of byte arrays");
        assertTrue(built.size() == 4, "Not enough nrpn messages to complete send change");

        byte[] expected0 = {(byte)0xb0,0x63,0x00};
        byte[] expected1 = {(byte)0xb0,0x62,0x01};
        byte[] expected2 = {(byte)0xb0,0x06,0x06};
        byte[] expected3 = {(byte)0xb0,0x26,0x02};

        assertArrayEquals(expected0,built.get(0));
        assertArrayEquals(expected1,built.get(1));
        assertArrayEquals(expected2,built.get(2));
        assertArrayEquals(expected3,built.get(3));
    }

    @Test
    public void testBuildNrpnlsbyte() throws Exception {
        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromResource("MidiControl/m7cl_sysex_mappings.json");
        List<NrpnMapping> nrpnMappings = NrpnMappingLoader.loadFromResource("MidiControl/nrpn/m7cl_nrpn_mappings.json");
        CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));
        registry.attachNrpnMappings(nrpnMappings);

        String canonicalId = "kInputFader.kFader.2";
        Optional<ControlInstance> instance = Optional.ofNullable(registry.resolve(canonicalId));

        assertNotNull(instance, "Registry should resolve canonical ID: " + canonicalId);

        NrpnMapping mapping = instance.get().getNrpn().get();
        assertNotNull(mapping, "Instance should have a SysexMapping attached");

        int testValue = 120;
        List<byte[]> built = mapping.buildNrpnBytes(instance,testValue);

        assertNotNull(built, "Builder should return a list of byte arrays");
        assertTrue(built.size() == 4, "Not enough nrpn messages to complete send change");

        byte[] expected0 = {(byte)0xb0,0x63,0x00};
        byte[] expected1 = {(byte)0xb0,0x62,0x02};
        byte[] expected2 = {(byte)0xb0,0x06,0x00};
        byte[] expected3 = {(byte)0xb0,0x26,0x78};

        assertArrayEquals(expected0,built.get(0));
        assertArrayEquals(expected1,built.get(1));
        assertArrayEquals(expected2,built.get(2));
        assertArrayEquals(expected3,built.get(3));
    }

    public static String bytesToHex(byte[] message) {
        StringBuilder sb = new StringBuilder();
        for (byte b : message) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}