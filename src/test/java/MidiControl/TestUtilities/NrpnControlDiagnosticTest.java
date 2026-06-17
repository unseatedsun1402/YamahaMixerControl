package MidiControl.TestUtilities;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
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

public class NrpnControlDiagnosticTest {

    @Test
    public void dumpM7clInputFaderControlModel() throws Exception {
        List<SysexMapping> sysexMappings =
                SysexMappingLoader.loadMappingsFromResource(
                        "MidiControl/m7cl_sysex_mappings.json"
                );

        List<NrpnMapping> nrpnMappings =
                NrpnMappingLoader.loadFromResource(
                        "MidiControl/nrpn/m7cl_nrpn_mappings.json"
                );

        CanonicalRegistry registry =
                new CanonicalRegistry(sysexMappings, new SysexParser(sysexMappings));

        registry.attachNrpnMappings(nrpnMappings);

        String canonicalId = "kInputFader.kFader.1";

        Optional<ControlInstance> maybeInstance =
                Optional.ofNullable(registry.resolve(canonicalId));

        assertTrue(
                maybeInstance.isPresent(),
                "Registry should resolve canonical ID: " + canonicalId
        );

        ControlInstance instance = maybeInstance.get();

        assertNotNull(instance.getSysex(), "Instance should have a SysEx mapping attached");
        assertTrue(instance.getNrpn().isPresent(), "Instance should have an NRPN mapping attached");

        SysexMapping sysex = instance.getSysex();
        NrpnMapping nrpn = instance.getNrpn().get();

        dumpControlInstance(instance);
        dumpSysexMapping(sysex);
        dumpNrpnMapping(nrpn);

        int[] testValues = {
                0,
                1,
                8,
                120,
                128,
                512,
                770,
                823,
                1023
        };

        for (int value : testValues) {
            dumpBuiltMessagesForValue(instance, sysex, nrpn, value);
        }
    }

    private static void dumpControlInstance(ControlInstance instance) {
        System.out.println();
        System.out.println("============================================================");
        System.out.println("CONTROL INSTANCE");
        System.out.println("============================================================");

        System.out.println("canonicalId     = " + instance.getCanonicalId());
        System.out.println("group           = " + instance.getGroup());
        System.out.println("subcontrol      = " + instance.getSubcontrol());
        System.out.println("index           = " + instance.getIndex());
        System.out.println("instanceIndex   = " + instance.getInstanceIndex());
        System.out.println("min             = " + instance.getMin());
        System.out.println("max             = " + instance.getMax());
        System.out.println("range           = " + (instance.getMax() - instance.getMin()));
        System.out.println("priority        = " + instance.getPriority());

        byte resolution = instance.getResolution();

        System.out.println(
                "getResolution() = 0x" +
                String.format("%02X", resolution & 0xFF) +
                " (" + describeResolution(resolution) + ")"
        );

        System.out.println("has NRPN        = " + instance.getNrpn().isPresent());
        System.out.println("has change map  = " + instance.hasChangeMapping());
        System.out.println("has request map = " + instance.hasRequestMapping());
    }

    private static void dumpSysexMapping(SysexMapping sysex) {
        System.out.println();
        System.out.println("============================================================");
        System.out.println("SYSEX MAPPING");
        System.out.println("============================================================");

        System.out.println("controlGroup            = " + sysex.getControlGroup());
        System.out.println("controlId               = " + sysex.getControl_id());
        System.out.println("maxChannels             = " + sysex.getMax_Channels());
        System.out.println("subControl              = " + sysex.getSubControl());
        System.out.println("semantics               = " + sysex.getSemantics());
        System.out.println("channelIndex            = " + sysex.getChannel_index());
        System.out.println("key                     = " + sysex.getKey());
        System.out.println("comment                 = " + sysex.getComment());

        System.out.println("value                   = " + sysex.getValue());
        System.out.println("minValue                = " + sysex.getMin_value());
        System.out.println("maxValue                = " + sysex.getMax_value());
        System.out.println("defaultValue            = " + sysex.getDefault_value());

        System.out.println("addressBytes            = " + Arrays.toString(sysex.getAddressBytes()));
        System.out.println("indexBytes              = " + Arrays.toString(sysex.getIndexBytes()));
        System.out.println("computed indexByteIdx   = " + Arrays.toString(sysex.getIndexByteIndices()));
        System.out.println("computed valueByteIdx   = " + Arrays.toString(sysex.getValueByteIndices()));

        System.out.println("parameterChangeFormat   = " + sysex.getParameter_change_format());
        System.out.println("parameterRequestFormat  = " + sysex.getParameter_request_format());
    }

    private static void dumpNrpnMapping(NrpnMapping nrpn) {
        System.out.println();
        System.out.println("============================================================");
        System.out.println("NRPN MAPPING");
        System.out.println("============================================================");

        System.out.println("canonicalId = " + nrpn.getCanonicalId());
        System.out.println("msb raw     = " + nrpn.getMsb());
        System.out.println("lsb raw     = " + nrpn.getLsb());
        System.out.println("msb int     = " + nrpn.msbInt());
        System.out.println("lsb int     = " + nrpn.lsbInt());
    }

    private static void dumpBuiltMessagesForValue(
            ControlInstance instance,
            SysexMapping sysex,
            NrpnMapping nrpn,
            int value
    ) {
        System.out.println();
        System.out.println("------------------------------------------------------------");
        System.out.println("VALUE TEST: canonical value = " + value);
        System.out.println("------------------------------------------------------------");

        byte[] sysexChange = sysex.buildChangeMessage(value, instance.getIndex());

        System.out.println("SysEx change:");
        System.out.println("  " + bytesToHex(sysexChange));

        List<byte[]> currentNrpn =
                nrpn.buildNrpnBytes(value);

        System.out.println("Current production NRPN buildNrpnBytes(...):");
        printMessages(currentNrpn);

        int msb = nrpn.msbInt();
        int lsb = nrpn.lsbInt();

        int value7From1023 = scale(value, 0, 1023, 0, 127);
        int value14From1023 = scale(value, 0, 1023, 0, 16383);

        int split10Msb = (value >> 7) & 0x7F;
        int split10Lsb = value & 0x7F;

        int split14Msb = (value14From1023 >> 7) & 0x7F;
        int split14Lsb = value14From1023 & 0x7F;

        System.out.println("Derived diagnostic values:");
        System.out.println("  scale 0..1023 -> 0..127   = " + value7From1023);
        System.out.println("  scale 0..1023 -> 0..16383 = " + value14From1023);
        System.out.println("  split raw 10-bit MSB      = " + split10Msb);
        System.out.println("  split raw 10-bit LSB      = " + split10Lsb);
        System.out.println("  split scaled 14-bit MSB   = " + split14Msb);
        System.out.println("  split scaled 14-bit LSB   = " + split14Lsb);

        System.out.println();
        System.out.println("Candidate A: current-style 10-bit split, MSB then LSB select");
        printMessages(List.of(
                cc(99, msb),
                cc(98, lsb),
                cc(6, split10Msb),
                cc(38, split10Lsb)
        ));

        System.out.println("Candidate B: current-style 10-bit split, LSB then MSB select");
        printMessages(List.of(
                cc(98, lsb),
                cc(99, msb),
                cc(6, split10Msb),
                cc(38, split10Lsb)
        ));

        System.out.println("Candidate C: scaled 7-bit value, MSB then LSB select, CC6 only");
        printMessages(List.of(
                cc(99, msb),
                cc(98, lsb),
                cc(6, value7From1023)
        ));

        System.out.println("Candidate D: scaled 7-bit value, LSB then MSB select, CC6 only");
        printMessages(List.of(
                cc(98, lsb),
                cc(99, msb),
                cc(6, value7From1023)
        ));

        System.out.println("Candidate E: old-server-like scaled 7-bit, LSB then MSB, CC6 + CC26 decimal");
        printMessages(List.of(
                cc(98, lsb),
                cc(99, msb),
                cc(6, value7From1023),
                cc(26, value7From1023)
        ));

        System.out.println("Candidate F: scaled 14-bit full NRPN, MSB then LSB select");
        printMessages(List.of(
                cc(99, msb),
                cc(98, lsb),
                cc(6, split14Msb),
                cc(38, split14Lsb)
        ));

        System.out.println("Candidate G: scaled 14-bit full NRPN, LSB then MSB select");
        printMessages(List.of(
                cc(98, lsb),
                cc(99, msb),
                cc(6, split14Msb),
                cc(38, split14Lsb)
        ));
    }

    private static byte[] cc(int controller, int value) {
        return new byte[] {
                (byte) 0xB0,
                (byte) (controller & 0x7F),
                (byte) (value & 0x7F)
        };
    }

    private static void printMessages(List<byte[]> messages) {
        for (int i = 0; i < messages.size(); i++) {
            System.out.println("  [" + i + "] " + bytesToHex(messages.get(i)));
        }
    }

    private static int scale(
            int value,
            int inMin,
            int inMax,
            int outMin,
            int outMax
    ) {
        if (value <= inMin) return outMin;
        if (value >= inMax) return outMax;

        double normalised = (value - inMin) / (double) (inMax - inMin);
        return (int) Math.round(outMin + normalised * (outMax - outMin));
    }

    private static String describeResolution(byte resolution) {
        int r = resolution & 0xFF;

        return switch (r) {
            case 0x0F -> "7-bit according to current ControlInstance logic";
            case 0xF0 -> "10-bit according to current ControlInstance logic";
            case 0xFF -> "14-bit / full according to current ControlInstance logic";
            default -> "unknown";
        };
    }

    public static String bytesToHex(byte[] message) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < message.length; i++) {
            if (i > 0) sb.append(" ");
            sb.append(String.format("%02X", message[i] & 0xFF));
        }

        return sb.toString();
    }
}