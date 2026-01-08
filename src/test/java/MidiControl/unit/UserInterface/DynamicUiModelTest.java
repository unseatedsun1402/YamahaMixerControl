package MidiControl.unit.UserInterface;

import MidiControl.ContextModel.*;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.SysexUtils.*;
import MidiControl.UserInterface.*;
import MidiControl.UserInterface.DTO.*;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DynamicUiModelTest {

    @Test
    public void testUiModelForChannel() {

        // ------------------------------------------------------------
        // 1. Load real mappings
        // ------------------------------------------------------------
        List<SysexMapping> mappings =
                SysexMappingLoader.loadMappingsFromResource("MidiControl/m7cl_sysex_mappings.json");

        SysexParser parser = new SysexParser(mappings);
        CanonicalRegistry registry = new CanonicalRegistry(mappings, parser);
        ControlSchema schema = new ControlSchema(registry);

        // Use the new compact builder
        ViewBuilder viewBuilder = new InputChannelStripViewBuilder(registry);

        UiContextIndex dummyIndex = new UiContextIndex();
        UiModelFactory factory = new UiModelFactory(registry, schema, viewBuilder, dummyIndex);

        // ------------------------------------------------------------
        // 2. Build UI model for channel.1
        // ------------------------------------------------------------
        UiModelDTO model = factory.buildUiModel("channel.1");

        assertNotNull(model, "UI model must not be null");
        assertEquals("channel.1", model.contextId);

        assertNotNull(model.controls, "Controls list must not be null");
        assertFalse(model.controls.isEmpty(), "Channel strip must have controls");

        // ------------------------------------------------------------
        // 3. Validate expected controls exist
        // ------------------------------------------------------------
        assertTrue(
                model.controls.stream().anyMatch(c -> "CHANNEL_ON".equals(c.logicId)),
                "CHANNEL_ON missing"
        );

        assertTrue(
                model.controls.stream().anyMatch(c -> "PAN".equals(c.logicId)),
                "PAN missing"
        );

        assertTrue(
                model.controls.stream().anyMatch(c -> c.logicId.startsWith("SEND_MIX")),
                "SEND_MIX{n} missing"
        );

        assertTrue(
                model.controls.stream().anyMatch(c -> "FADER".equals(c.logicId)),
                "FADER missing"
        );

        // ------------------------------------------------------------
        // 4. Validate canonical identity
        // ------------------------------------------------------------
        for (ViewControlDTO vc : model.controls) {

            // Canonical identity must exist
            assertNotNull(vc.hwGroup, "hwGroup must not be null");
            assertNotNull(vc.hwSubcontrol, "hwSubcontrol must not be null");

            // hwInstance must be >= 0
            assertTrue(vc.hwInstance >= 0, "hwInstance must be >= 0");

            // Canonical ID must contain at least two dots
            String canonicalId = vc.hwGroup + "." + vc.hwSubcontrol + "." + vc.hwInstance;
            long dotCount = canonicalId.chars().filter(ch -> ch == '.').count();
            assertTrue(dotCount == 2, "Canonical ID must be group.sub.index: " + canonicalId);

            // Canonical ID must end with a number
            assertTrue(
                    Character.isDigit(canonicalId.charAt(canonicalId.length() - 1)),
                    "Canonical ID must end with an index: " + canonicalId
            );
        }
    }
}