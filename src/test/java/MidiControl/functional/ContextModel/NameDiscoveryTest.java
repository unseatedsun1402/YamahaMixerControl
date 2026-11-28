package MidiControl.functional.ContextModel;

import MidiControl.ContextModel.*;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Collectors;

public class NameDiscoveryTest {

    private long countContextsForGroup(
        List<Context> contexts,
        String groupName)
    {
        return contexts.stream()
            .filter(c -> c.getContextType() == ContextType.NAME)
            .filter(c -> c.getFilters().stream()
                .anyMatch(f -> groupName.equals(f.getControlGroup())))
            .count();
    }

    @Test
    public void test01V96INameDiscovery() {

        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");
        CanonicalRegistry registry = new CanonicalRegistry(mappings,new SysexParser(mappings));

        ContextDiscoveryEngine engine = new ContextDiscoveryEngine(registry);

        List<Context> contexts = engine.discoverContexts();

        List<Context> nameContexts = contexts.stream()
            .filter(ctx -> ctx.getContextType() == ContextType.NAME)
            .collect(Collectors.toList());

        assertFalse(nameContexts.isEmpty(), "No name contexts discovered!");

        boolean hasMixNames = nameContexts.stream()
            .anyMatch(ctx -> ctx.getId().startsWith("name.") &&
                             ctx.getFilters().stream().anyMatch(f -> f.getControlGroup().contains("kMix")));

        boolean hasDcaNames = nameContexts.stream()
            .anyMatch(ctx -> ctx.getId().startsWith("name.") &&
                             ctx.getFilters().stream().anyMatch(f -> f.getControlGroup().contains("kDCA")));
        
        assertFalse(hasMixNames,"01V96i should not expose Mix name contexts");
        assertFalse(hasDcaNames,"01V96i should not expose DCA name contexts");

        assertEquals(32,
            countContextsForGroup(nameContexts, "kInputChannelName"));

        assertEquals(8,
            countContextsForGroup(nameContexts, "kAUXModuleName"));

        assertEquals(8,
            countContextsForGroup(nameContexts, "kBusModuleName"));

        boolean hasShortNames = nameContexts.stream()
            .flatMap(ctx -> ctx.getFilters().stream())
            .anyMatch(f -> f.getSubControl().contains("Short"));

        boolean hasLongNames = nameContexts.stream()
            .flatMap(ctx -> ctx.getFilters().stream())
            .anyMatch(f -> f.getSubControl().contains("Long") ||
                           f.getControlGroup().contains("NameMixModule"));

        assertTrue(hasShortNames, "Short (4 char) names not detected!");
        assertTrue(hasLongNames, "Long (8–16 char) names not detected!");
    }

    @Test
    public void testM7CLNameDiscovery() {

        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromResource("MidiControl/m7cl_sysex_mappings.json");
        CanonicalRegistry registry = new CanonicalRegistry(mappings,new SysexParser(mappings));

        ContextDiscoveryEngine engine = new ContextDiscoveryEngine(registry);

        List<Context> contexts = engine.discoverContexts();

        List<Context> nameContexts = contexts.stream()
            .filter(ctx -> ctx.getContextType() == ContextType.NAME)
            .collect(Collectors.toList());

        assertFalse(nameContexts.isEmpty(), "No name contexts discovered!");

        assertEquals(56,
            countContextsForGroup(nameContexts, "kNameInputChannel"));

        assertEquals(8,
            countContextsForGroup(nameContexts, "kDCAName"));

        assertEquals(16,
            countContextsForGroup(nameContexts, "kNameMixModule"));

        assertEquals(8,
            countContextsForGroup(nameContexts, "kNameMatrixModule"));

        assertEquals(56, nameContexts.stream()
            .filter(ctx -> ctx.getFilters().stream()
                .anyMatch(f ->
                    f.getControlGroup().equals("kNameInputChannel") &&
                    f.getSubControl().equals("kNameShort1")
                )
            )
            .count(),
            "Expected 56 M7CL input channel name contexts");

        boolean hasShortNames = nameContexts.stream()
            .flatMap(ctx -> ctx.getFilters().stream())
            .anyMatch(f -> f.getSubControl().contains("Short"));
        assertTrue(hasShortNames, "Short (4 char) names not detected!");
    }
}
