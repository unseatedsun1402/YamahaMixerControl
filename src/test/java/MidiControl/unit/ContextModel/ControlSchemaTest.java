package MidiControl.unit.ContextModel;

import MidiControl.ContextModel.ControlSchema;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ControlSchemaTest {

        @Test
        public void testSchemaExtraction() {
        List<SysexMapping> mappings =
                SysexMappingLoader.loadMappingsFromResource("MidiControl/m7cl_sysex_mappings.json");

        SysexParser parser = new SysexParser(mappings);
        CanonicalRegistry registry = new CanonicalRegistry(mappings, parser);

        ControlSchema schema = new ControlSchema(registry);

        // Fader group should contain prefix "kFader"
        Set<String> faderPrefixes = schema.getPrefixesForGroup("kInputFader");
        assertTrue(faderPrefixes.contains("kFader"),
                "kInputFader should contain prefix kFader");

        // EQ group should contain prefix "kEQ"
        Set<String> eqPrefixes = schema.getPrefixesForGroup("kInputEQ");
        assertTrue(eqPrefixes.contains("kEQ"),
                "kInputEQ should contain prefix kEQ");

        // MixSend group should contain prefix "kMixSend"
        Set<String> mixPrefixes = schema.getPrefixesForGroup("kInputToMix");
        assertTrue(mixPrefixes.contains("kMix"),
                "kInputMixSend should contain prefix kMixSend");

        // Reverse lookup: prefix → groups
        Set<String> groupsForEQ = schema.getGroupsForPrefix("kEQ");
        assertTrue(groupsForEQ.contains("kInputEQ"),
                "Prefix kEQ should map back to group kInputEQ");

        Set<String> groupsForFader = schema.getGroupsForPrefix("kFader");
        assertTrue(groupsForFader.contains("kInputFader"),
                "Prefix kFader should map back to group kInputFader");

        System.out.println("---- Prefixes per Group ----");
        registry.getGroups().forEach((group, g) -> {
                System.out.println(group + " → " + schema.getPrefixesForGroup(group));
        });

        System.out.println("---- Groups per Prefix ----");
        schema.getAllPrefixes().forEach(prefix -> {
                System.out.println(prefix + " → " + schema.getGroupsForPrefix(prefix));
        });
        }

        @Test
        public void testSchemaExtraction_01V96i() {
                // 1. Load the 01V96i sysex mappings
                List<SysexMapping> mappings =
                        SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");

                SysexParser parser = new SysexParser(mappings);
                CanonicalRegistry registry = new CanonicalRegistry(mappings, parser);

                // 2. Build the schema
                ControlSchema schema = new ControlSchema(registry);

                // ------------------------------------------------------------
                // 3. Validate REAL prefixes from the REAL JSON you provided
                // ------------------------------------------------------------

                // kInputAUX should contain prefix "kAUX"
                Set<String> auxPrefixes = schema.getPrefixesForGroup("kInputAUX");
                assertTrue(auxPrefixes.contains("kAUX"),
                        "kInputAUX should contain prefix kAUX");

                // kInputFader should contain prefix "kFader"
                Set<String> faderPrefixes = schema.getPrefixesForGroup("kInputFader");
                assertTrue(faderPrefixes.contains("kFader"),
                        "kInputFader should contain prefix kFader");

                // Reverse lookup: prefix → groups
                Set<String> groupsForAUX = schema.getGroupsForPrefix("kAUX");
                assertTrue(groupsForAUX.contains("kInputAUX"),
                        "Prefix kAUX should map back to group kInputAUX");

                Set<String> groupsForFader = schema.getGroupsForPrefix("kFader");
                assertTrue(groupsForFader.contains("kInputFader"),
                        "Prefix kFader should map back to group kInputFader");

                // ------------------------------------------------------------
                // 4. Print for visual inspection
                // ------------------------------------------------------------
                System.out.println("---- Prefixes per Group (01V96i) ----");
                registry.getGroups().forEach((group, g) -> {
                        System.out.println(group + " → " + schema.getPrefixesForGroup(group));
                });

                System.out.println("---- Groups per Prefix (01V96i) ----");
                schema.getAllPrefixes().forEach(prefix -> {
                        System.out.println(prefix + " → " + schema.getGroupsForPrefix(prefix));
                });
        }
}
