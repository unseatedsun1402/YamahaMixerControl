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
        List<SysexMapping> mappings =
            SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");

        List<NrpnMapping> nrpnMappings =
            NrpnMappingLoader.loadFromResource("MidiControl/nrpn/01v96i_nrpn_mappings.json");

        CanonicalRegistry registry =
            new CanonicalRegistry(mappings, new SysexParser(mappings));

        registry.attachNrpnMappings(nrpnMappings);

        String canonicalId = "kInputFader.kFader.1";
        ControlInstance instance = registry.resolve(canonicalId);

        assertNotNull(instance, "Registry should resolve canonical ID: " + canonicalId);

        SysexMapping mapping = instance.getSysex();

        assertNotNull(mapping, "Instance should have a SysexMapping attached");

        int testValue = 770;
        byte[] built = mapping.buildChangeMessage(testValue, instance.getIndex());

        assertNotNull(built, "Builder should return a SysEx byte array");

        byte[] expected = {
            (byte) 0xF0,
            0x43,
            0x10,
            0x3E,
            0x7F,
            0x01,
            0x1C,
            0x00,
            0x01,
            0x00,
            0x00,
            0x06,
            0x02,
            (byte) 0xF7
        };

        assertArrayEquals(expected, built);
    }

    // @Test
    public void testBuildM7clNrpnCc6OnlyForFader1() throws Exception {
        TestContext ctx = loadM7cl("kInputFader.kFader.1");

        int testValue = 770;
        int ccValue = scale1023To7Bit(testValue);

        List<byte[]> built = ctx.nrpn.buildNrpnBytes(ccValue);
        assertNotNull(built);
        assertTrue(built.size() == 3);

        assertArrayEquals(new byte[]{(byte) 0xB0, 0x63, 0x00}, built.get(0));
        assertArrayEquals(new byte[]{(byte) 0xB0, 0x62, 0x01}, built.get(1));
        assertArrayEquals(new byte[]{(byte) 0xB0, 0x06, (byte) ccValue}, built.get(2));
    }

    // @Test
    public void testBuildM7clNrpnCc6OnlyForFader2() throws Exception {
        TestContext ctx = loadM7cl("kInputFader.kFader.2");

        int testValue = 120;
        int ccValue = scale1023To7Bit(testValue);

        List<byte[]> built = ctx.nrpn.buildNrpnBytes(ccValue);

        assertNotNull(built);
        assertTrue(built.size() == 3);

        assertArrayEquals(new byte[]{(byte) 0xB0, 0x63, 0x00}, built.get(0));
        assertArrayEquals(new byte[]{(byte) 0xB0, 0x62, 0x02}, built.get(1));
        assertArrayEquals(new byte[]{(byte) 0xB0, 0x06, (byte) scale1023To7Bit(testValue)}, built.get(2));
    }

    private static TestContext loadM7cl(String canonicalId) throws Exception {
        List<SysexMapping> mappings =
            SysexMappingLoader.loadMappingsFromResource("MidiControl/m7cl_sysex_mappings.json");

        List<NrpnMapping> nrpnMappings =
            NrpnMappingLoader.loadFromResource("MidiControl/nrpn/m7cl_nrpn_mappings.json");

        CanonicalRegistry registry =
            new CanonicalRegistry(mappings, new SysexParser(mappings));

        registry.attachNrpnMappings(nrpnMappings);

        Optional<ControlInstance> instance =
            Optional.ofNullable(registry.resolve(canonicalId));

        assertTrue(instance.isPresent(), "Registry should resolve canonical ID: " + canonicalId);
        assertTrue(instance.get().getNrpn().isPresent(), "Instance should have NRPN mapping");

        return new TestContext(instance.get(), instance.get().getNrpn().get());
    }

    private static int scale1023To7Bit(int value) {
        if (value <= 0) {
            return 0;
        }

        if (value >= 1023) {
            return 127;
        }

        return (int) Math.round(value * 127.0 / 1023.0);
    }

    private record TestContext(ControlInstance instance, NrpnMapping nrpn) {}
}