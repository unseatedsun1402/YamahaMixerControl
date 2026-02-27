package MidiControl.functional.HardwareToServer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import javax.sound.midi.ShortMessage;

import org.junit.jupiter.api.Test;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;
import MidiControl.Mocks.MockMidiServer;
import MidiControl.NrpnUtils.NrpnMapping;
import MidiControl.NrpnUtils.NrpnMappingLoader;
import MidiControl.NrpnUtils.NrpnParser;
import MidiControl.NrpnUtils.NrpnRegistry;
import MidiControl.Server.MidiServer;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;

public class SendNrpnChangeTest {
    @Test
    public void testBuildNrpn2byte() throws Exception {
        System.out.println("\n===== testBuildNrpnFader1 =====\n");
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

        System.out.println("Built nrpn " + bytesToHex(built.get(0))+" " + bytesToHex(built.get(1))+
        " " + bytesToHex(built.get(2))+" " + bytesToHex(built.get(3)));

        assertArrayEquals(expected0,built.get(0));
        assertArrayEquals(expected1,built.get(1));
        assertArrayEquals(expected2,built.get(2));
        assertArrayEquals(expected3,built.get(3));

        MidiServer server = new MockMidiServer(registry);
        NrpnParser testParser = new NrpnParser();
        NrpnRegistry testRegistry = new NrpnRegistry();
        testRegistry.loadFromClasspath("MidiControl/nrpn/m7cl_nrpn_mappings.json");

        server.addtoinputqueue(new ShortMessage(expected0[0], expected0[1], expected0[2]));
        server.addtoinputqueue(new ShortMessage(expected1[0], expected1[1], expected1[2]));
        server.addtoinputqueue(new ShortMessage(expected2[0], expected2[1], expected2[2]));
        server.addtoinputqueue(new ShortMessage(expected3[0], expected3[1], expected3[2]));

        MockMidiServer.setNrpnFields(testParser,testRegistry);

        server.processIncomingMidiForTest();
        assertEquals(testValue, instance.get().getValue());
    }

    @Test
    public void testTransmissionInteruptionTrucatesValue() throws Exception {
        System.out.println("\n===== testBuildNrpnFader1 =====\n");
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
        byte[] expected3 = {(byte)0xb0,0x26,0x02}; // nrpn for 770

        System.out.println("Built nrpn " + bytesToHex(built.get(0))+" " + bytesToHex(built.get(1))+
        " " + bytesToHex(built.get(2))+" " + bytesToHex(built.get(3)));

        assertArrayEquals(expected0,built.get(0));
        assertArrayEquals(expected1,built.get(1));
        assertArrayEquals(expected2,built.get(2));
        assertArrayEquals(expected3,built.get(3));

        MidiServer server = new MockMidiServer(registry);
        NrpnParser testParser = new NrpnParser();
        NrpnRegistry testRegistry = new NrpnRegistry();
        testRegistry.loadFromClasspath("MidiControl/nrpn/m7cl_nrpn_mappings.json");

        server.addtoinputqueue(new ShortMessage(expected0[0], expected0[1], expected0[2]));
        server.addtoinputqueue(new ShortMessage(expected1[0], expected1[1], expected1[2]));
        server.addtoinputqueue(new ShortMessage(expected2[0], expected2[1], expected2[2]));
        server.addtoinputqueue(new ShortMessage(ShortMessage.CONTROL_CHANGE,44,45)); //interupt transmission
        server.addtoinputqueue(new ShortMessage(ShortMessage.NOTE_OFF,44,45)); //interupt transmission
        server.addtoinputqueue(new ShortMessage(expected3[0], expected3[1], expected3[2]));

        // now it will only read to msb and skip lsb outputting a value of 6 not 770
        int expectedValue = 768;

        MockMidiServer.setNrpnFields(testParser,testRegistry);

        server.processIncomingMidiForTest();

        assertEquals(expectedValue, instance.get().getValue());
    }

    @Test
    public void testBuildNrpn1byte() throws Exception {
        System.out.println("\n===== testBuildNrpnFader1 =====\n");
        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromResource("MidiControl/m7cl_sysex_mappings.json");
        List<NrpnMapping> nrpnMappings = NrpnMappingLoader.loadFromResource("MidiControl/nrpn/m7cl_nrpn_mappings.json");
        CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));
        registry.attachNrpnMappings(nrpnMappings);

        String canonicalId = "kInputFader.kFader.1";
        Optional<ControlInstance> instance = Optional.ofNullable(registry.resolve(canonicalId));

        assertNotNull(instance, "Registry should resolve canonical ID: " + canonicalId);

        NrpnMapping mapping = instance.get().getNrpn().get();
        assertNotNull(mapping, "Instance should have a SysexMapping attached");

        int testValue = 65;
        List<byte[]> built = mapping.buildNrpnBytes(instance,testValue);

        assertNotNull(built, "Builder should return a list of byte arrays");
        assertTrue(built.size() == 4, "Not enough nrpn messages to complete send change");

        byte[] expected0 = {(byte)0xb0,0x63,0x00};
        byte[] expected1 = {(byte)0xb0,0x62,0x01};
        byte[] expected2 = {(byte)0xb0,0x06,0x0};
        byte[] expected3 = {(byte)0xb0,0x26,(byte)0x41};

        System.out.println("Built nrpn " + bytesToHex(built.get(0))+" " + bytesToHex(built.get(1))+
        " " + bytesToHex(built.get(2))+" " + bytesToHex(built.get(3)));

        assertArrayEquals(expected0,built.get(0));
        assertArrayEquals(expected1,built.get(1));
        assertArrayEquals(expected2,built.get(2));
        assertArrayEquals(expected3,built.get(3));

        MidiServer server = new MockMidiServer(registry);
        NrpnParser testParser = new NrpnParser();
        NrpnRegistry testRegistry = new NrpnRegistry();
        testRegistry.loadFromClasspath("MidiControl/nrpn/m7cl_nrpn_mappings.json");

        server.addtoinputqueue(new ShortMessage(expected0[0], expected0[1], expected0[2]));
        server.addtoinputqueue(new ShortMessage(expected1[0], expected1[1], expected1[2]));
        server.addtoinputqueue(new ShortMessage(expected2[0], expected2[1], expected2[2]));
        server.addtoinputqueue(new ShortMessage(expected3[0], expected3[1], expected3[2]));

        MockMidiServer.setNrpnFields(testParser,testRegistry);

        server.processIncomingMidiForTest();
        assertEquals(testValue, instance.get().getValue());
    }



    public static String bytesToHex(byte[] message) {
        StringBuilder sb = new StringBuilder();
        for (byte b : message) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
