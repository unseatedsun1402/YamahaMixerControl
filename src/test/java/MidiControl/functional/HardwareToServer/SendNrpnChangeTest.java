package MidiControl.functional.HardwareToServer;

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

public class SendNrpnChangeTest {

    @Test
    public void testM7clBuildNrpnCc6OnlyForMidHighFaderValue() throws Exception {
        TestContext ctx = load(
            "MidiControl/m7cl_sysex_mappings.json",
            "MidiControl/nrpn/m7cl_nrpn_mappings.json",
            "kInputFader.kFader.1"
        );

        int testValue = 770;
        int scaled14 = scale1023To7Bit(testValue);
        List<byte[]> built = ctx.nrpn.buildNrpnBytes(scaled14);

        assertNotNull(built);
        assertTrue(built.size() == 3);

        assertArrayEquals(new byte[]{(byte) 0xB0, 0x63, 0x00}, built.get(0));
        assertArrayEquals(new byte[]{(byte) 0xB0, 0x62, 0x01}, built.get(1));
        assertArrayEquals(new byte[]{(byte) 0xB0, 0x06, (byte) scaled14}, built.get(2));
    }

    @Test
    public void testM7clBuildNrpnCc6OnlyForLowFaderValue() throws Exception {
        TestContext ctx = load(
            "MidiControl/m7cl_sysex_mappings.json",
            "MidiControl/nrpn/m7cl_nrpn_mappings.json",
            "kInputFader.kFader.1"
        );

        int testValue = 65;
        int scaled14 = scale1023To7Bit(testValue);
        List<byte[]> built = ctx.nrpn.buildNrpnBytes(scaled14);

        assertNotNull(built);
        assertTrue(built.size() == 3);

        assertArrayEquals(new byte[]{(byte) 0xB0, 0x63, 0x00}, built.get(0));
        assertArrayEquals(new byte[]{(byte) 0xB0, 0x62, 0x01}, built.get(1));
        assertArrayEquals(new byte[]{(byte) 0xB0, 0x06, (byte) scaled14}, built.get(2));
    }

    @Test
    public void testM7clBuildNrpnCc6OnlyForMaxFaderValue() throws Exception {
        TestContext ctx = load(
            "MidiControl/m7cl_sysex_mappings.json",
            "MidiControl/nrpn/m7cl_nrpn_mappings.json",
            "kInputFader.kFader.1"
        );

        int testValue = 1023;
        List<byte[]> built = ctx.nrpn.buildNrpnBytes(testValue);

        assertNotNull(built);
        assertTrue(built.size() == 3);

        assertArrayEquals(new byte[]{(byte) 0xB0, 0x63, 0x00}, built.get(0));
        assertArrayEquals(new byte[]{(byte) 0xB0, 0x62, 0x01}, built.get(1));
        assertArrayEquals(new byte[]{(byte) 0xB0, 0x06, 0x7F}, built.get(2));
    }

    @Test
    public void testM7clBuildNrpnCc6OnlyForMinFaderValue() throws Exception {
        TestContext ctx = load(
            "MidiControl/m7cl_sysex_mappings.json",
            "MidiControl/nrpn/m7cl_nrpn_mappings.json",
            "kInputFader.kFader.1"
        );

        int testValue = 0;
        List<byte[]> built = ctx.nrpn.buildNrpnBytes(testValue);

        assertNotNull(built);
        assertTrue(built.size() == 3);

        assertArrayEquals(new byte[]{(byte) 0xB0, 0x63, 0x00}, built.get(0));
        assertArrayEquals(new byte[]{(byte) 0xB0, 0x62, 0x01}, built.get(1));
        assertArrayEquals(new byte[]{(byte) 0xB0, 0x06, 0x00}, built.get(2));
    }

    @Test
    public void test01v96iBuildNrpn14bitForMidHighFaderValue() throws Exception {
        TestContext ctx = load(
            "MidiControl/01v96i_sysex_mappings.json",
            "MidiControl/nrpn/01v96i_nrpn_mappings.json",
            "kInputFader.kFader.1"
        );

        int testValue = 770;
        int scaled14 = scale1023To14Bit(testValue);

        List<byte[]> built = ctx.nrpn.buildNrpnBytes(scaled14);

        assertNotNull(built);
        assertTrue(built.size() == 4);

        assertArrayEquals(new byte[]{(byte) 0xB0, 0x63, 0x00}, built.get(0));
        assertArrayEquals(new byte[]{(byte) 0xB0, 0x62, 0x01}, built.get(1));
        assertArrayEquals(new byte[]{(byte) 0xB0, 0x06, (byte) ((scaled14 >> 7) & 0x7F)}, built.get(2));
        assertArrayEquals(new byte[]{(byte) 0xB0, 0x26, (byte) (scaled14 & 0x7F)}, built.get(3));
    }

    @Test
    public void test01v96iBuildNrpn14bitForLowFaderValue() throws Exception {
        TestContext ctx = load(
            "MidiControl/01v96i_sysex_mappings.json",
            "MidiControl/nrpn/01v96i_nrpn_mappings.json",
            "kInputFader.kFader.1"
        );

        int testValue = 65;
        int scaled14 = scale1023To14Bit(testValue);

        List<byte[]> built = ctx.nrpn.buildNrpnBytes(scaled14);

        assertNotNull(built);
        assertTrue(built.size() == 4);

        assertArrayEquals(new byte[]{(byte) 0xB0, 0x63, 0x00}, built.get(0));
        assertArrayEquals(new byte[]{(byte) 0xB0, 0x62, 0x01}, built.get(1));
        assertArrayEquals(new byte[]{(byte) 0xB0, 0x06, (byte) ((scaled14 >> 7) & 0x7F)}, built.get(2));
        assertArrayEquals(new byte[]{(byte) 0xB0, 0x26, (byte) (scaled14 & 0x7F)}, built.get(3));
    }

    @Test
    public void test01v96iBuildNrpn14bitForMaxFaderValue() throws Exception {
        TestContext ctx = load(
            "MidiControl/01v96i_sysex_mappings.json",
            "MidiControl/nrpn/01v96i_nrpn_mappings.json",
            "kInputFader.kFader.1"
        );

        int testValue = 1023;
        int scaled14 = scale1023To14Bit(testValue);
        List<byte[]> built = ctx.nrpn.buildNrpnBytes(scaled14);

        assertNotNull(built);
        assertTrue(built.size() == 4);

        assertArrayEquals(new byte[]{(byte) 0xB0, 0x63, 0x00}, built.get(0));
        assertArrayEquals(new byte[]{(byte) 0xB0, 0x62, 0x01}, built.get(1));
        assertArrayEquals(new byte[]{(byte) 0xB0, 0x06, 0x7F}, built.get(2));
        assertArrayEquals(new byte[]{(byte) 0xB0, 0x26, 0x7F}, built.get(3));
    }

    @Test
    public void test01v96iBuildNrpn14bitForMinFaderValue() throws Exception {
        TestContext ctx = load(
            "MidiControl/01v96i_sysex_mappings.json",
            "MidiControl/nrpn/01v96i_nrpn_mappings.json",
            "kInputFader.kFader.1"
        );

        int testValue = 0;
        List<byte[]> built = ctx.nrpn.buildNrpnBytes(testValue);

        assertNotNull(built);
        assertTrue(built.size() == 4);

        assertArrayEquals(new byte[]{(byte) 0xB0, 0x63, 0x00}, built.get(0));
        assertArrayEquals(new byte[]{(byte) 0xB0, 0x62, 0x01}, built.get(1));
        assertArrayEquals(new byte[]{(byte) 0xB0, 0x06, 0x00}, built.get(2));
        assertArrayEquals(new byte[]{(byte) 0xB0, 0x26, 0x00}, built.get(3));
    }

    private static TestContext load(
        String sysexResource,
        String nrpnResource,
        String canonicalId
    ) throws Exception {
        List<SysexMapping> mappings =
            SysexMappingLoader.loadMappingsFromResource(sysexResource);

        List<NrpnMapping> nrpnMappings =
            NrpnMappingLoader.loadFromResource(nrpnResource);

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
        if (value <= 0) return 0;
        if (value >= 1023) return 127;

        return (int) Math.round(value * 127.0 / 1023.0);
    }

    private static int scale1023To14Bit(int value) {
        if (value <= 0) return 0;
        if (value >= 1023) return 16383;

        return (int) Math.round(value * 16383.0 / 1023.0);
    }

    private record TestContext(ControlInstance instance, NrpnMapping nrpn) {}
}