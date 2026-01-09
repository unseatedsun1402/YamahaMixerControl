package MidiControl.unit.ContextModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import MidiControl.ContextModel.BankContext;
import MidiControl.ContextModel.BankFilter;
import MidiControl.ContextModel.Context;
import MidiControl.ContextModel.ContextDiscoveryEngine;
import MidiControl.ContextModel.ContextType;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;

public class ContextDiscoveryTest {
    @Test
    public void testChannelDiscovery() {

        String json =
            "[{" +
                "\"control_group\": \"kInputFader\"," +
                "\"control_id\": 50," +
                "\"sub_control\": \"kFader\"," +
                "\"max_channels\": 4," +
                "\"value\": 0," +
                "\"min_value\": 0," +
                "\"max_value\": 1023," +
                "\"default_value\": 0," +
                "\"comment\": \"PRM TABLE #03\"," +
                "\"key\": 12345," +
                "\"parameter_change_format\": [240,67,\"1n\",62,17,1,0,50,0,0,\"cc\",\"cc\",\"dd\",\"dd\",\"dd\",\"dd\",\"dd\",247]," +
                "\"parameter_request_format\": [240,67,\"3n\",62,17,1,0,50,0,0,\"cc\",\"cc\",247]" +
            "}]";

        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromString(json);
        CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));

        ContextDiscoveryEngine engine = new ContextDiscoveryEngine(registry);
        List<Context> contexts = engine.discoverContexts();

        // Expect 4 channels: channel.0, channel.1, channel.2, channel.3
        long channelCount = contexts.stream()
            .filter(c -> c.getId().startsWith("channel."))
            .count();

        assertEquals(4, channelCount, "Expected 4 channel contexts");
    }

    @Test
    public void testMixDiscovery_01V96i() {

        String json =
            "[" +
            "{" +
                "\"control_group\": \"kAUXFader\"," +
                "\"control_id\": 34," +
                "\"max_channels\": 8," +
                "\"sub_control\": \"kFader\"," +
                "\"value\": 0," +
                "\"min_value\": 0," +
                "\"max_value\": 1023," +
                "\"default_value\": 0," +
                "\"comment\": \"PRM TABLE #05-2\"," +
                "\"key\": 268418753282," +
                "\"parameter_change_format\": [240,67,\"1n\",62,127,1,35,2,\"cc\",\"dd\",\"dd\",\"dd\",\"dd\",247]," +
                "\"parameter_request_format\": [240,67,\"3n\",62,127,1,35,2,\"cc\",247]" +
            "}" +
            "]";

        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromString(json);
        CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));

        ContextDiscoveryEngine engine = new ContextDiscoveryEngine(registry);
        List<Context> contexts = engine.discoverContexts();

        boolean aux1Found = contexts.stream()
            .anyMatch(c -> c.getId().equals("aux.1"));

        assertTrue(aux1Found, "Expected aux.1 context from kAUXFader");
    }

    @Test
    public void testMatrixDiscovery_M7CL() {

        String json =
            "[" +
            "{" +
                "\"control_group\": \"kMatrixFader\"," +
                "\"control_id\": 68," +
                "\"max_channels\": 8," +
                "\"sub_control\": \"kFader\"," +
                "\"value\": 0," +
                "\"min_value\": 0," +
                "\"max_value\": 1023," +
                "\"default_value\": 0," +
                "\"comment\": \"PRM TABLE #03\"," +
                "\"key\": 266573250628," +
                "\"parameter_change_format\": [240,67,\"1n\",62,17,1,0,68,0,5,\"cc\",\"cc\",\"dd\",\"dd\",\"dd\",\"dd\",\"dd\",247]," +
                "\"parameter_request_format\": [240,67,\"3n\",62,17,1,0,68,0,5,\"cc\",\"cc\",247]" +
            "}" +
            "]";

        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromString(json);
        CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));

        ContextDiscoveryEngine engine = new ContextDiscoveryEngine(registry);
        List<Context> contexts = engine.discoverContexts();

        boolean matrix1Found = contexts.stream()
            .anyMatch(c -> c.getId().equals("matrix.1"));

        assertTrue(matrix1Found, "Expected matrix.1 context from kMatrixFader");
    }

    @Test
    public void testFullDiscoveryEngine() {

        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromResource("MidiControl/m7cl_sysex_mappings.json");

        CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));
        ContextDiscoveryEngine engine = new ContextDiscoveryEngine(registry);

        List<Context> contexts = engine.discoverContexts();

        long channels = contexts.stream().filter(c -> c.getId().startsWith("channel.")).count();
        long buses    = contexts.stream().filter(c -> c.getId().startsWith("mix")).count();

        assertTrue(channels > 0, "Expected channels");
        assertTrue(buses > 0, "Expected buses");
    }

    // Helper to build a minimal Context
    private Context ctx(String id, ContextType type) {
        return new Context(
                id,
                id,
                type,
                List.of(),
                List.of()
        );
    }

    // Helper to build a BankContext with filters
    private BankContext bank(BankFilter... filters) {
        BankContext bc =  new BankContext();
        for(BankFilter fileter : filters){
            bc.addFilter(fileter);
        }
        return bc;
    }

    // Engine with empty registry (matchesFilters does not use registry)
    private ContextDiscoveryEngine engine() {
        return new ContextDiscoveryEngine(new CanonicalRegistry(List.of(), null));
    }

    @Test
    public void testPrefixMatch() {
        Context ctx = ctx("channel.5", ContextType.CHANNEL);
        BankContext bank = bank(new BankFilter("channel", null, null));

        assertTrue(engine().matchesFilters(ctx, bank));
    }

    @Test
    public void testPrefixMismatch() {
        Context ctx = ctx("mix.2", ContextType.MIX);
        BankContext bank = bank(new BankFilter("channel", null, null));

        assertFalse(engine().matchesFilters(ctx, bank));
    }

    @Test
    public void testIndexMatch() {
        Context ctx = ctx("channel.7", ContextType.CHANNEL);
        BankContext bank = bank(new BankFilter("channel", 7, null));

        assertTrue(engine().matchesFilters(ctx, bank));
    }

    @Test
    public void testIndexMismatch() {
        Context ctx = ctx("channel.3", ContextType.CHANNEL);
        BankContext bank = bank(new BankFilter("channel", 5, null));

        assertFalse(engine().matchesFilters(ctx, bank));
    }

    @Test
    public void testTypeMatch() {
        Context ctx = ctx("mix.1", ContextType.MIX);
        BankContext bank = bank(new BankFilter(null, null, ContextType.MIX));

        assertTrue(engine().matchesFilters(ctx, bank));
    }

    @Test
    public void testTypeMismatch() {
        Context ctx = ctx("mix.1", ContextType.MIX);
        BankContext bank = bank(new BankFilter(null, null, ContextType.CHANNEL));

        assertFalse(engine().matchesFilters(ctx, bank));
    }

    @Test
    public void testMultipleFiltersOneMatches() {
        Context ctx = ctx("channel.4", ContextType.CHANNEL);

        BankFilter bad = new BankFilter("mix", null, null);
        BankFilter good = new BankFilter("channel", 4, ContextType.CHANNEL);

        BankContext bank = bank(bad, good);

        assertTrue(engine().matchesFilters(ctx, bank));
    }

    @Test
    public void testMultipleFiltersNoneMatch() {
        Context ctx = ctx("channel.4", ContextType.CHANNEL);

        BankFilter f1 = new BankFilter("mix", null, null);
        BankFilter f2 = new BankFilter("aux", null, null);

        BankContext bank = bank(f1, f2);

        assertFalse(engine().matchesFilters(ctx, bank));
    }

    @Test
    public void testMalformedContextId() {
        Context ctx = ctx("invalid", ContextType.CHANNEL);
        BankContext bank = bank(new BankFilter("invalid", 3, null));

        // Should not crash, should simply not match
        assertFalse(engine().matchesFilters(ctx, bank));
    }

    @Test
    public void testFilterWithAllNullFieldsMatchesEverything() {
        Context ctx = ctx("anything.123", ContextType.MATRIX);
        BankContext bank = bank(new BankFilter(null, null, null));

        assertTrue(engine().matchesFilters(ctx, bank));
    }

}
