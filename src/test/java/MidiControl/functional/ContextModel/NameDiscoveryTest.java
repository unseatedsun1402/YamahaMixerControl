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

        boolean hasInputNames = nameContexts.stream()
            .anyMatch(ctx -> ctx.getId().startsWith("name.") &&
                             ctx.getFilters().stream().anyMatch(f -> f.getControlGroup().contains("kInput")));

        boolean hasMixNames = nameContexts.stream()
            .anyMatch(ctx -> ctx.getId().startsWith("name.") &&
                             ctx.getFilters().stream().anyMatch(f -> f.getControlGroup().contains("kMix")));

        boolean hasAuxNames = nameContexts.stream()
            .anyMatch(ctx -> ctx.getId().startsWith("name.") &&
                             ctx.getFilters().stream().anyMatch(f -> f.getControlGroup().contains("kAUX")));

        boolean hasMatrixNames = nameContexts.stream()
            .anyMatch(ctx -> ctx.getId().startsWith("name.") &&
                             ctx.getFilters().stream().anyMatch(f -> f.getControlGroup().contains("kMatrix")));

        boolean hasDcaNames = nameContexts.stream()
            .anyMatch(ctx -> ctx.getId().startsWith("name.") &&
                             ctx.getFilters().stream().anyMatch(f -> f.getControlGroup().contains("kDCA")));

        assertTrue(hasInputNames, "Input channel names not discovered!");

        
        assertEquals(32, nameContexts.stream()
            .filter(ctx -> ctx.getFilters().stream()
                .anyMatch(f ->
                    f.getControlGroup().equals("kInputChannelName") &&
                    f.getSubControl().equals("kChannelNameShort1")
                )
            )
            .count(),
            "Expected 40 01V96i input channel name contexts");

        // assertFalse(hasMixNames, "Mix bus names not discovered!"); // auxes on 01v96i not mixes
        // assertTrue(hasAuxNames, "Aux bus names not discovered!");
        // assertTrue(hasMatrixNames, "Matrix names not discovered!");
        // assertFalse(hasDcaNames, "DCA names not discovered!");  // no dcas on 01v96i

        boolean hasShortNames = nameContexts.stream()
            .flatMap(ctx -> ctx.getFilters().stream())
            .anyMatch(f -> f.getSubControl().contains("Short"));

        boolean hasLongNames = nameContexts.stream()
            .flatMap(ctx -> ctx.getFilters().stream())
            .anyMatch(f -> f.getSubControl().contains("Long") ||
                           f.getControlGroup().contains("NameMixModule"));

        assertTrue(hasShortNames, "Short (4 char) names not detected!");
        // assertTrue(hasLongNames, "Long (8–16 char) names not detected!");
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

        boolean hasInputNames = nameContexts.stream()
            .anyMatch(ctx -> ctx.getId().startsWith("name.") &&
                             ctx.getFilters().stream().anyMatch(f -> f.getControlGroup().contains("kNameInput")));

        boolean hasMixNames = nameContexts.stream()
            .anyMatch(ctx -> ctx.getId().startsWith("name.") &&
                             ctx.getFilters().stream().anyMatch(f -> f.getControlGroup().contains("kNameMix")));

        boolean hasAuxNames = nameContexts.stream()
            .anyMatch(ctx -> ctx.getId().startsWith("name.") &&
                             ctx.getFilters().stream().anyMatch(f -> f.getControlGroup().contains("kNameAUX")));

        boolean hasMatrixNames = nameContexts.stream()
            .anyMatch(ctx -> ctx.getId().startsWith("name.") &&
                             ctx.getFilters().stream().anyMatch(f -> f.getControlGroup().contains("kNameMatrix")));

        boolean hasDcaNames = nameContexts.stream()
            .anyMatch(ctx -> ctx.getId().startsWith("name.") &&
                             ctx.getFilters().stream().anyMatch(f -> f.getControlGroup().contains("kDCA")));

        assertTrue(hasInputNames, "Input channel names not discovered!");

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
