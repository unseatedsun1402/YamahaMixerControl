package MidiControl.functional.ContextModel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import MidiControl.ContextModel.Context;
import MidiControl.ContextModel.ContextFactory;
import MidiControl.ContextModel.InputChannelStripViewBuilder;
import MidiControl.ContextModel.ViewBuilder;
import MidiControl.ContextModel.ViewControl;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;

public class ContextFactoryTest {

    @Test
    public void testCompactChannelStrip_M7CL() {

        List<SysexMapping> mappings =
                SysexMappingLoader.loadMappingsFromResource("MidiControl/m7cl_sysex_mappings.json");

        SysexParser parser = new SysexParser(mappings);
        CanonicalRegistry registry = new CanonicalRegistry(mappings, parser);

        // Build channel 1 context
        Context ctx = ContextFactory.buildChannelStrip(registry, 1);

        assertNotNull(ctx);
        assertEquals("channel.1", ctx.getId());

        // Build compact view
        ViewBuilder builder = new InputChannelStripViewBuilder();
        List<ViewControl> controls = builder.build(ctx, registry, null);

        assertFalse(controls.isEmpty(), "Compact view should not be empty");

        // Expect CHANNEL_ON
        assertTrue(
                controls.stream().anyMatch(c -> c.logicId.equals("CHANNEL_ON")),
                "CHANNEL_ON missing"
        );

        // Expect PAN
        assertTrue(
                controls.stream().anyMatch(c -> c.logicId.equals("PAN")),
                "PAN missing"
        );

        // Expect at least one SEND_MIX
        assertTrue(
                controls.stream().anyMatch(c -> c.logicId.startsWith("SEND_MIX")),
                "SEND_MIX{n} missing"
        );

        // Expect FADER
        assertTrue(
                controls.stream().anyMatch(c -> c.logicId.equals("FADER")),
                "Fader missing"
        );

        // Ensure EQ is NOT present
        assertFalse(
                controls.stream().anyMatch(c -> c.logicId.startsWith("EQ")),
                "EQ controls should NOT appear in compact view"
        );
    }

    @Test
    public void testCompactChannelStrip_01V96i() {

        List<SysexMapping> mappings =
                SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");

        SysexParser parser = new SysexParser(mappings);
        CanonicalRegistry registry = new CanonicalRegistry(mappings, parser);

        Context ctx = ContextFactory.buildChannelStrip(registry, 1);
        assertNotNull(ctx);

        ViewBuilder builder = new InputChannelStripViewBuilder();
        List<ViewControl> controls = builder.build(ctx, registry, null);

        assertFalse(controls.isEmpty(), "Compact view should not be empty");

        // Expect SEND_MIX1 (01V96i AUX1)
        assertTrue(
                controls.stream().anyMatch(c -> c.logicId.equals("SEND_MIX1")),
                "SEND_MIX1 missing"
        );

        // Expect FADER
        assertTrue(
                controls.stream().anyMatch(c -> c.logicId.equals("FADER")),
                "Fader missing"
        );

        // Ensure no EQ
        assertFalse(
                controls.stream().anyMatch(c -> c.logicId.startsWith("EQ")),
                "EQ controls should NOT appear in compact view"
        );
    }
}