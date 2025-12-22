package MidiControl.unit.SysexUtils;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;

public class SysexParserNoMappingsTest {
    @Test
    public void testSysexFail() {
        assertThrows(RuntimeException.class, () -> SysexMappingLoader.loadMappingsFromResource("MidiControl/does_not_exist.json"));
    }
}