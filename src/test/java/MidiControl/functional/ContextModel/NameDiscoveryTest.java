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
    public void testNameDiscovery() {

        // -------------------------------------
        // 1. Load a real registry
        // -------------------------------------
        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");
        CanonicalRegistry registry = new CanonicalRegistry(mappings,new SysexParser(mappings));

        // -------------------------------------
        // 2. Build a discovery engine with the new discoverer
        // -------------------------------------
        ContextDiscoveryEngine engine = new ContextDiscoveryEngine(registry);

        // Manually add the NameContextDiscoverer into the engine
        engine.addDiscoverer(new NameContextDiscoverer());

        // -------------------------------------
        // 3. Run discovery
        // -------------------------------------
        List<Context> contexts = engine.discoverContexts();

        // Filter contexts of TYPE.NAME
        List<Context> nameContexts = contexts.stream()
            .filter(ctx -> ctx.getContextType() == ContextType.NAME)
            .collect(Collectors.toList());

        assertFalse(nameContexts.isEmpty(), "No name contexts discovered!");

        // -------------------------------------
        // 4. Validate Input, Mix/Aux, Matrix, DCA naming controls exist
        // -------------------------------------
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
        // assertFalse(hasMixNames, "Mix bus names not discovered!"); // auxes on 01v96i not mixes
        // assertTrue(hasAuxNames, "Aux bus names not discovered!");
        // assertTrue(hasMatrixNames, "Matrix names not discovered!");
        // assertFalse(hasDcaNames, "DCA names not discovered!");  // no dcas on 01v96i

        // -------------------------------------
        // 5. Validate short and long naming variants are included
        // -------------------------------------
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
    public void testOtherNameDiscovery() {

        // -------------------------------------
        // 1. Load a real registry
        // -------------------------------------
        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromResource("MidiControl/m7cl_sysex_mappings.json");
        CanonicalRegistry registry = new CanonicalRegistry(mappings,new SysexParser(mappings));

        // -------------------------------------
        // 2. Build a discovery engine with the new discoverer
        // -------------------------------------
        ContextDiscoveryEngine engine = new ContextDiscoveryEngine(registry);

        // Manually add the NameContextDiscoverer into the engine
        engine.addDiscoverer(new NameContextDiscoverer());

        // -------------------------------------
        // 3. Run discovery
        // -------------------------------------
        List<Context> contexts = engine.discoverContexts();

        // Filter contexts of TYPE.NAME
        List<Context> nameContexts = contexts.stream()
            .filter(ctx -> ctx.getContextType() == ContextType.NAME)
            .collect(Collectors.toList());

        assertFalse(nameContexts.isEmpty(), "No name contexts discovered!");

        // -------------------------------------
        // 4. Validate Input, Mix/Aux, Matrix, DCA naming controls exist
        // -------------------------------------
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
        // assertTrue(hasMixNames, "Mix bus names not discovered!");
        // assertFalse(hasAuxNames, "Aux bus names not discovered!"); // no auxwes on m7cl
        // assertTrue(hasMatrixNames, "Matrix names not discovered!");
        // assertTrue(hasDcaNames, "DCA names not discovered!");

        // -------------------------------------
        // 5. Validate short and long naming variants are included
        // -------------------------------------
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
}
