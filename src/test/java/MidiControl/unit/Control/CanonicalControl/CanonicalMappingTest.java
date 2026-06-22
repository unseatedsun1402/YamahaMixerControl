package MidiControl.unit.Control.CanonicalControl;

import static org.junit.jupiter.api.Assertions.*;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.SubControl;
import MidiControl.Mocks.FakeSysexMapping;
import MidiControl.Controls.ControlInstance;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;
import MidiControl.TestUtilities.TestListener;

import java.util.List;

import org.junit.jupiter.api.Test;

public class CanonicalMappingTest {

    @Test
    public void testCanonicalIdExpansion() {

        List<SysexMapping> mappings = FakeSysexMapping.fakeSysexMapping();

        // CanonicalRegistry builds the entire hierarchy automatically
        CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));

        ControlGroup group = registry.getGroup("kInputHA");
        assertNotNull(group, "Expected control group kInputHA");

        SubControl sub = group.getSubcontrol("kHAPhantom");
        assertNotNull(sub, "Expected subcontrol kHAPhantom");

        List<ControlInstance> instances = sub.getInstances();
        assertEquals(56, instances.size(), "Expected 3 instances from max_ch = 3");

        // Validate canonical IDs (0-based indexing)
        assertEquals("kInputHA.kHAPhantom.0", instances.get(0).getCanonicalId());
        assertEquals("kInputHA.kHAPhantom.1", instances.get(1).getCanonicalId());
        assertEquals("kInputHA.kHAPhantom.2", instances.get(2).getCanonicalId());

        // Validate parent relationships
        ControlInstance ci0 = instances.get(0);
        assertEquals("kHAPhantom", ci0.getParent().getName());
        assertEquals("kInputHA", ci0.getParent().getParentGroup().getName());
    }

    @Test
    public void updateControlTest() {
        List<SysexMapping> mappings = FakeSysexMapping.fakeSysexMapping();

        // Build canonical registry from mapping
        CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));

        // Retrieve the real generated instance
        ControlInstance ci = registry
            .getGroup("kInputHA")
            .getSubcontrol("kHAPhantom")
            .getInstances()
            .get(0);

        TestListener listener = new TestListener();
        ci.addListener(listener);

        assertDoesNotThrow(() -> ci.updateValue(127));

        assertEquals(127, listener.lastValue);
        assertEquals(ci, listener.lastInstance);
    }

    @Test
    public void getAllInstancesForContextTest() {
        List<SysexMapping> mappings =
            SysexMappingLoader.loadMappingsFromResource("MidiControl/m7cl_sysex_mappings.json");

        CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));

        int totalInstances = registry.getGroups().values().stream()
            .flatMap(group -> group.getSubcontrols().values().stream())
            .mapToInt(subgroup -> subgroup.getInstances().size())
            .sum();

        System.out.println("[TEST] Total instances for in registry" + ": " + totalInstances);
    }
}