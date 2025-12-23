package MidiControl.unit;

import static org.junit.jupiter.api.Assertions.*;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;

import java.util.List;

import javax.sound.midi.MidiUnavailableException;

import org.junit.jupiter.api.Test;

public class CanonicalRegistryTest {

    @Test
    void testResolveValidMessageByStringJson() {
        String json =
            "[{"
                + "\"control_group\": \"kInputHA\","
                + "\"control_id\": 41,"
                + "\"sub_control\": \"kHAPhantom\","
                + "\"max_channels\": 56,"
                + "\"value\": 0,"
                + "\"min_value\": 0,"
                + "\"max_value\": 1,"
                + "\"default_value\": 0,"
                + "\"comment\": \"Off, On\","
                + "\"key\": 266573250601,"
                + "\"parameter_change_format\": ["
                + "240, 67, \"1n\", 62, 17, 1, 0, 41, 0, 0,"
                + "\"cc\", \"cc\","
                + "\"dd\", \"dd\", \"dd\", \"dd\", \"dd\","
                + "247"
                + "],"
                + "\"parameter_request_format\": ["
                + "240, 67, \"3n\", 62, 17, 1, 0, 41, 0, 0,"
                + "\"cc\", \"cc\","
                + "247"
                + "]"
            + "}]";

        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromString(json);
        CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));

        byte[] msg = new byte[] {
            (byte)0xF0, (byte)0x43, (byte)0x10, (byte)0x3E,
            (byte)0x11, (byte)0x01, (byte)0x00,
            (byte)0x29, (byte)0x00, (byte)0x00,   // address bytes (ignored for index)
            (byte)0x29, (byte)0x00,               // cc, cc → index = 41
            (byte)0x01, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00,
            (byte)0xF7
        };

        try {
            ControlInstance ci = registry.resolveSysex(msg);
            assertNotNull(ci, "Expected ControlInstance to be resolved");

            assertEquals("kInputHA.kHAPhantom.41", ci.getCanonicalId());
            assertEquals("kInputHA", ci.getParent().getParentGroup().getName());
            assertEquals("kHAPhantom", ci.getParent().getName());

        } catch (MidiUnavailableException e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    void testResolveValidMessageByAllJson() {
        String json = "[{"
            + "\"control_group\": \"kInputFader\","
            + "\"control_id\": 50,"
            + "\"max_channels\": 55,"
            + "\"sub_control\": \"kFader\","
            + "\"value\": 0,"
            + "\"min_value\": 0,"
            + "\"max_value\": 1023,"
            + "\"default_value\": 0,"
            + "\"comment\": \"PRM TABLE #03\","
            + "\"key\": 266573250610,"
            + "\"parameter_change_format\": ["
            + "240, 67, \"1n\", 62, 17, 1, 0, 50, 0, 0,"
            + "\"cc\", \"cc\","
            + "\"dd\", \"dd\", \"dd\", \"dd\", \"dd\","
            + "247"
            + "],"
            + "\"parameter_request_format\": ["
            + "240, 67, \"3n\", 62, 17, 1, 0, 50, 0, 0,"
            + "\"cc\", \"cc\","
            + "247"
            + "]"
        + "}]";

        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromString(json);
        CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));

        byte[] faderMsg = {
            (byte)0xF0, (byte)0x43, (byte)0x10, (byte)0x3E,
            (byte)0x11, (byte)0x01, (byte)0x00,
            (byte)0x32, (byte)0x00, (byte)0x00,   // control_id = 50
            (byte)0x1B, (byte)0x00,               // cc, cc → index = 27
            (byte)0x14, (byte)0x03, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0xF7
        };

        try {
            ControlInstance ci = registry.resolveSysex(faderMsg);
            assertNotNull(ci, "Expected ControlInstance to be resolved");

            assertEquals("kInputFader", ci.getParent().getParentGroup().getName());
            assertEquals("kFader", ci.getParent().getName());
            assertEquals("kInputFader.kFader.27", ci.getCanonicalId());

        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    void testResolveSysexReturnsNullWhenParserReturnsNull() throws Exception {
        CanonicalRegistry registry = new CanonicalRegistry(List.of(),new SysexParser(List.of()));

        byte[] msg = new byte[] { (byte)0xF0, 0x01, 0x02, (byte)0xF7 }; // nonsense

        ControlInstance ci = registry.resolveSysex(msg);
        assertNull(ci, "Expected null when parser cannot match any mapping");
    }

    @Test
    void testResolveSysexMissingControlGroup() throws Exception {
        String json =
            "[{"
                + "\"control_group\": \"kDoesNotExist\","
                + "\"control_id\": 1,"
                + "\"max_channels\": 1,"
                + "\"sub_control\": \"kTest\","
                + "\"parameter_change_format\": [240,67,\"1n\",62,17,1,0,1,0,0,\"cc\",\"cc\",\"dd\",\"dd\",\"dd\",\"dd\",\"dd\",247],"
                + "\"parameter_request_format\": [240,67,\"3n\",62,17,1,0,1,0,0,\"cc\",\"cc\",247]"
            + "}]";

        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromString(json);
        CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));

        byte[] msg = new byte[] {
            (byte)0xF0, 0x43, 0x10, 0x3E,
            0x11, 0x01, 0x00,
            0x01, 0x00, 0x00,
            0x00, 0x00,
            0x00,0x00,0x00,0x00,0x00,
            (byte)0xF7
        };

        ControlInstance ci = registry.resolveSysex(msg);
        assertNull(ci, "Expected null when control group does not exist");
    }

    @Test
    void testResolveSysexMissingSubcontrol() throws Exception {
        String json =
            "[{"
                + "\"control_group\": \"kInputHA\","
                + "\"control_id\": 1,"
                + "\"max_channels\": 1,"
                + "\"sub_control\": \"kDoesNotExist\","
                + "\"parameter_change_format\": [240,67,\"1n\",62,17,1,0,1,0,0,\"cc\",\"cc\",\"dd\",\"dd\",\"dd\",\"dd\",\"dd\",247],"
                + "\"parameter_request_format\": [240,67,\"3n\",62,17,1,0,1,0,0,\"cc\",\"cc\",247]"
            + "}]";

        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromString(json);
        CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));

        byte[] msg = new byte[] {
            (byte)0xF0, 0x43, 0x10, 0x3E,
            0x11, 0x01, 0x00,
            0x01, 0x00, 0x00,
            0x00, 0x00,
            0x00,0x00,0x00,0x00,0x00,
            (byte)0xF7
        };

        ControlInstance ci = registry.resolveSysex(msg);
        assertNull(ci, "Expected null when subcontrol does not exist");
    }

    @Test
    void testResolveSysexIndexOutOfRange() throws Exception {
        String json = "[{"
            + "\"control_group\": \"kInputFader\","
            + "\"control_id\": 50,"
            + "\"max_channels\": 55,"
            + "\"sub_control\": \"kFader\","
            + "\"value\": 0,"
            + "\"min_value\": 0,"
            + "\"max_value\": 1023,"
            + "\"default_value\": 0,"
            + "\"comment\": \"PRM TABLE #03\","
            + "\"key\": 266573250610,"
            + "\"parameter_change_format\": ["
            + "240, 67, \"1n\", 62, 17, 1, 0, 50, 0, 0,"
            + "\"cc\", \"cc\","
            + "\"dd\", \"dd\", \"dd\", \"dd\", \"dd\","
            + "247"
            + "],"
            + "\"parameter_request_format\": ["
            + "240, 67, \"3n\", 62, 17, 1, 0, 50, 0, 0,"
            + "\"cc\", \"cc\","
            + "247"
            + "]"
        + "}]";

        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromString(json);
        CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));

        byte[] msg = {
            (byte)0xF0, (byte)0x43, (byte)0x10, (byte)0x3E,
            (byte)0x11, (byte)0x01, (byte)0x00,
            (byte)0x32, (byte)0x00, (byte)0x00,   // control_id = 50
            (byte)0x7F, (byte)0x7F,               // cc, cc → index = massive
            (byte)0x14, (byte)0x03, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0xF7
        };

        assertThrows(IndexOutOfBoundsException.class, () -> registry.resolveSysex(msg));
    }
}