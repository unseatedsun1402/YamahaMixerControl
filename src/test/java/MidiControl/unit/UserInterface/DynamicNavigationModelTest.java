package MidiControl.unit.UserInterface;

import MidiControl.ContextModel.*;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.SysexUtils.*;
import MidiControl.UserInterface.NavigationModel.NavigationCategory;
import MidiControl.UserInterface.NavigationModel.NavigationContextFactory;
import MidiControl.UserInterface.NavigationModel.NavigationItem;
import MidiControl.UserInterface.NavigationModel.NavigationModel;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class DynamicNavigationModelTest {

    @Test
    public void testNavigationModel() {

        // ------------------------------------------------------------
        // 1. Load real mappings (M7CL, 01V96i, etc.)
        // ------------------------------------------------------------
        List<SysexMapping> mappings =
                SysexMappingLoader.loadMappingsFromResource("MidiControl/m7cl_sysex_mappings.json");

        SysexParser parser = new SysexParser(mappings);
        CanonicalRegistry registry = new CanonicalRegistry(mappings, parser);
        ControlSchema schema = new ControlSchema(registry);

        NavigationContextFactory factory = new NavigationContextFactory(registry, schema);

        // ------------------------------------------------------------
        // 2. Build navigation model
        // ------------------------------------------------------------
        NavigationModel nav = factory.buildNavigation();

        assertNotNull(nav, "Navigation model must not be null");
        assertFalse(nav.navigation.isEmpty(), "Navigation categories must not be empty");

        // ------------------------------------------------------------
        // 3. Validate each category
        // ------------------------------------------------------------
        for (NavigationCategory cat : nav.navigation) {

            assertNotNull(cat.category, "Category name must not be null");
            assertFalse(cat.category.isBlank(), "Category name must not be blank");

            assertNotNull(cat.contexts, "Contexts list must not be null");
            assertFalse(cat.contexts.isEmpty(),
                    "Category " + cat.category + " must contain at least one context");

            // Validate each context entry
            for (NavigationItem item : cat.contexts) {

                assertNotNull(item.id, "Context ID must not be null");
                assertNotNull(item.label, "Context label must not be null");

                // ID must contain a dot (e.g., channel.1)
                assertTrue(item.id.contains("."),
                        "Context ID must contain a dot: " + item.id);

                // Extract prefix and index
                String[] parts = item.id.split("\\.");
                assertEquals(2, parts.length,
                        "Context ID must be in form prefix.index: " + item.id);

                String prefix = parts[0];
                String indexStr = parts[1];

                // Index must be numeric
                assertDoesNotThrow(() -> Integer.parseInt(indexStr),
                        "Context index must be numeric: " + item.id);

                int index = Integer.parseInt(indexStr);
                assertTrue(index > 0, "Context index must be > 0");

                // Label must contain the index
                assertTrue(item.label.contains(Integer.toString(index)),
                        "Label must contain index: " + item.label);

                // Label must start with a capitalized prefix (CH, Mix, DCA, etc.)
                assertTrue(Character.isUpperCase(item.label.charAt(0)),
                        "Label must start with uppercase: " + item.label);
            }
        }

        // ------------------------------------------------------------
        // 4. Validate counts match registry
        // ------------------------------------------------------------
        Map<String, Integer> expectedCounts = new HashMap<>();
        expectedCounts.put("Inputs", schema.detectIndexedRangeForAnyGroup("kFader"));
        expectedCounts.put("Mixes", schema.detectIndexedRangeForAnyGroup("kMix"));
        expectedCounts.put("Matrices", schema.detectIndexedRangeForAnyGroup("kMatrix"));
        expectedCounts.put("DCAs", schema.detectIndexedRangeForAnyGroup("kDCAName"));
        expectedCounts.put("Stereo Inputs", schema.detectIndexedRangeForAnyGroup("kStereoInChannelName"));

        for (NavigationCategory cat : nav.navigation) {
            int expected = expectedCounts.getOrDefault(cat.category, 0);
            assertEquals(expected, cat.contexts.size(),
                    "Category count mismatch for " + cat.category);
        }
    }
}