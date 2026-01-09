package MidiControl.unit.UserInterface;

import MidiControl.ContextModel.*;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;
import MidiControl.UserInterface.*;
import MidiControl.UserInterface.DTO.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DynamicUiModelTestForAuxMix {

    @Test
    public void testUiModelForAuxMix() {

        List<SysexMapping> mappings =
                SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");

        SysexParser parser = new SysexParser(mappings);
        CanonicalRegistry registry = new CanonicalRegistry(mappings, parser);

        ContextDiscoveryEngine discovery = new ContextDiscoveryEngine(registry);
        List<Context> contexts = discovery.discoverContexts();

        contexts.stream()
            .filter(c -> c.getId().startsWith("aux."))
            .forEach(c -> {
                System.out.println("Context: " + c.getId());
                c.getFilters().forEach(f ->
                    System.out.println("  Filter: " + f.getControlGroup() + "." + f.getSubControl() + "." + f.getIndex())
                );
            });


        UiContextIndex index = new UiContextIndex();
        index.addAll(contexts);

        ViewRegistry views = new ViewRegistry();
        views.addView(new MixAuxBusViewBuilder(registry), "basic-master-view");

        UiModelFactory factory = new UiModelFactory(
                registry,
                views.getView("basic-master-view").orElseThrow(),
                index
        );

        UiModelDTO model = factory.buildUiModel("aux.3");

        assertNotNull(model);
        assertEquals("aux.3", model.contextId);
        assertNotNull(model.controls);
        assertFalse(model.controls.isEmpty());

        assertTrue(model.controls.stream().anyMatch(c -> "FADER".equals(c.logicId)));
        assertTrue(model.controls.stream().anyMatch(c -> "PAN".equals(c.logicId)));
        assertTrue(model.controls.stream().anyMatch(c -> c.logicId.startsWith("DYN_")));
        assertTrue(model.controls.stream().anyMatch(c -> c.logicId.startsWith("EQ_")));

        for (ViewControlDTO vc : model.controls) {
            assertNotNull(vc.hwGroup);
            assertNotNull(vc.hwSubcontrol);
            assertTrue(vc.hwInstance >= 0);

            String canonicalId = vc.hwGroup + "." + vc.hwSubcontrol + "." + vc.hwInstance;
            long dotCount = canonicalId.chars().filter(ch -> ch == '.').count();
            assertEquals(2, dotCount);
            assertTrue(Character.isDigit(canonicalId.charAt(canonicalId.length() - 1)));
        }
    }
}