package MidiControl.unit.CanonicalRegistry;

import static org.junit.jupiter.api.Assertions.*;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;
import MidiControl.Mocks.FakeSysexMapping;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexParser;

import java.util.List;

import org.junit.jupiter.api.Test;

public class CanonicalRegistryTest {

    @Test
    void testResolveValidMessage() throws Exception {

        List<SysexMapping> mappings = FakeSysexMapping.fakeSysexMapping();

        CanonicalRegistry registry =
            new CanonicalRegistry(mappings, new SysexParser(mappings));

        byte[] msg = FakeSysexMapping.testRequest();

        System.out.println("Mapping Key: "+ Long.toString(mappings.get(0).getKey()) );

        ControlInstance ci = registry.resolveSysex(msg);
        assertNotNull(ci);

        assertEquals("kInputHA.kHAPhantom.41", ci.getCanonicalId());
        assertEquals("kInputHA", ci.getParent().getParentGroup().getName());
        assertEquals("kHAPhantom", ci.getParent().getName());
    }

    @Test
    void testResolveSysexReturnsNullWhenParserReturnsNull() throws Exception {
        // Real mappings for registry (so metadata exists)
        List<SysexMapping> mappings = FakeSysexMapping.fakeSysexMapping();

        // Parser with NO mappings, but still needs metadata to initialize
        SysexParser parser = new SysexParser(mappings);

        CanonicalRegistry registry = new CanonicalRegistry(mappings, parser);

        byte[] msg = { (byte)0xF0, 0x01, 0x02, (byte)0xF7 };

        assertNull(registry.resolveSysex(msg));
    }

    @Test
    void testResolveSysexMissingControlGroup() throws Exception {

        List<SysexMapping> mappings = FakeSysexMapping.fakeSysexMapping();

        CanonicalRegistry registry =
            new CanonicalRegistry(mappings, new SysexParser(mappings));

        // Wrong control group → should not match
        byte[] msg = {
            (byte)0xF0, 0x43, 0x10, 0x3E,
            0x11, 0x01, 0x00,
            0x01, 0x00, 0x00,
            0x00, 0x00,
            0x00,0x00,0x00,0x00,0x00,
            (byte)0xF7
        };

        System.out.println("Mapping Key: "+ Long.toString(mappings.get(0).getKey()) );

        assertNull(registry.resolveSysex(msg));
    }

    @Test
    void testResolveSysexIndexOutOfRange() throws Exception {

        List<SysexMapping> mappings = FakeSysexMapping.fakeSysexMapping();

        CanonicalRegistry registry =
            new CanonicalRegistry(mappings, new SysexParser(mappings));

        // index_bytes = [10,11] → 0x7F7F = 16383 (way out of range)
        byte[] msg = {
            (byte)0xF0, (byte)0x43, (byte)0x10, (byte)0x3E,
            (byte)0x11, (byte)0x01, (byte)0x00,
            (byte)0x29, (byte)0x00, (byte)0x00,
            (byte)0x7F, (byte)0x7F,
            (byte)0x14, (byte)0x03, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0xF7
        };

        System.out.println("Mapping Key: "+ Long.toString(mappings.get(0).getKey()) );

        assertNull(registry.resolveSysex(msg));
    }
}