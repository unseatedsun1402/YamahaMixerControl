package MidiControl.unit.NrpnUtils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import MidiControl.NrpnUtils.NrpnMapping;

@Tag("unit")
public class NrpnMappingTest {
    @Test
    public void initialiseMapping(){
        NrpnMapping mapping = new NrpnMapping("1","2","canonical.mapping.1");
        assertEquals("canonical.mapping.1", mapping.getCanonicalId());
        assertEquals("1", mapping.getMsb());
        assertEquals("2", mapping.getLsb());
    }

    @Test
    public void testGetMsbLsbAsInt(){
        NrpnMapping mapping = new NrpnMapping("1","2","canonical.mapping.1");
        assertEquals(1, mapping.msbInt());
        assertEquals(2, mapping.lsbInt());
    }

    @Test
    public void testBuildNrpnBytes(){
        NrpnMapping mapping = new NrpnMapping("1","2","canonical.mapping.1");
        List<byte[]> nrpnBytes = mapping.buildNrpnBytes(Optional.empty(),0);
        assertArrayEquals(new byte[]{-80,99,1}, nrpnBytes.get(0));
        assertArrayEquals(new byte[]{-80,98,2}, nrpnBytes.get(1));
        assertArrayEquals(new byte[]{-80,6,0}, nrpnBytes.get(2));
    }
}
