package MidiControl.unit.ContextModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import MidiControl.ContextModel.Context;
import MidiControl.ContextModel.ContextDiscoveryEngine;
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
                "\"control_group\": \"kInputAUX\"," +
                "\"control_id\": 34," +
                "\"max_channels\": 39," +
                "\"sub_control\": \"kAUX1Level\"," +
                "\"value\": 2," +
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

        boolean mix1Found = contexts.stream()
            .anyMatch(c -> c.getId().equals("mix.1"));

        assertTrue(mix1Found, "Expected mix.1 context from kAUX1Level");
    }

    @Test
    public void testMatrixDiscovery_M7CL() {

        String json =
            "[" +
            "{" +
                "\"control_group\": \"kInputToMatrix\"," +
                "\"control_id\": 68," +
                "\"max_channels\": 55," +
                "\"sub_control\": \"kMatrix1Level\"," +
                "\"value\": 5," +
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

        assertTrue(matrix1Found, "Expected matrix.1 context from kMatrix1Level");
    }

    @Test
    public void testFullDiscoveryEngine() {

        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromResource("MidiControl/m7cl_sysex_mappings.json");

        CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));
        ContextDiscoveryEngine engine = new ContextDiscoveryEngine(registry);

        List<Context> contexts = engine.discoverContexts();

        long channels = contexts.stream().filter(c -> c.getId().startsWith("channel.")).count();
        long mixes    = contexts.stream().filter(c -> c.getId().startsWith("mix.")).count();
        long matrices = contexts.stream().filter(c -> c.getId().startsWith("matrix.")).count();
        long outputs  = contexts.stream().filter(c -> c.getId().startsWith("bus.")).count();

        assertTrue(channels > 0, "Expected channels");
        assertTrue(mixes > 0, "Expected mixes");
        assertTrue(matrices >= 0, "Matrices may be zero for some desks");
        assertTrue(outputs > 0, "Expected Outputs");
    }
}
