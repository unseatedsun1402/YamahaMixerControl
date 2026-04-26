package MidiControl.unit.ContextModel;

import MidiControl.ContextModel.*;
import MidiControl.Controls.*;
import MidiControl.Mocks.MockCanonicalRegistry;
import MidiControl.SysexUtils.SysexMapping;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;


public class InputChannelSendsOnFaderViewBuilderTest {

    private ControlGroup makeGroup(String name, String sub, int instances) {
        ControlGroup g = new ControlGroup(name);
        SubControl sc = new SubControl(g, sub);

        for (int i = 0; i < instances; i++) {
            SysexMapping map = dummyMapping(name, sub, 0, 127);
            ControlInstance ci = new ControlInstance(sc, i, map, null);
            sc.addInstance(ci);
        }

        g.getSubcontrols().put(sub, sc);
        return g;
    }

    private ControlGroup makeGroupMulti(String group, String... subcontrols) {
        ControlGroup g = new ControlGroup(group);
        for (String sub : subcontrols) {
            SubControl sc = new SubControl(g, sub);
            SysexMapping map = dummyMapping(group, sub, 0, 127);
            ControlInstance ci = new ControlInstance(sc, 0, map, null);
            sc.addInstance(ci);
            g.getSubcontrols().put(sub, sc);
        }
        return g;
    }

    private SysexMapping dummyMapping(String group, String sub, int min, int max) {
        return new SysexMapping(
                group,
                0,
                1,
                sub,
                null,
                0,
                0L,
                new int[]{0},
                new int[]{0},
                min,
                min,
                max,
                min,
                "test",
                List.of("F0", "00"),
                List.of("F0", "00"),
                3
        );
    }

    private Context channel0() {
        return new Context("channel.0", "Channel 1", ContextType.CHANNEL, List.of(), List.of());
    }

    @Test
    public void testSendsOnFaderMix1() {
        MockCanonicalRegistry registry = new MockCanonicalRegistry();

        ControlGroup on = makeGroup("kInputOn", "kChannelOn", 1);
        ControlGroup pan = makeGroup("kInputPan", "kChannelPan", 1);

        ControlGroup send1 = makeGroup("kInputToMix", "kMix1Level", 1);
        ControlGroup send2 = makeGroup("kInputToMix", "kMix2Level", 1);

        registry.getGroups().put("kInputOn", on);
        registry.getGroups().put("kInputPan", pan);
        registry.getGroups().put("kInputToMix", send1);
        registry.getGroups().put("kInputToMix", send2);

        registry.mapContext("channel.0", on, pan, send1, send2);

        InputChannelSendsOnFaderViewBuilder builder = new InputChannelSendsOnFaderViewBuilder();
        List<ViewControl> controls = builder.build(channel0(), registry, "mix1");

        assertTrue(contains(controls, "CHANNEL_ON"));
        assertTrue(contains(controls, "PAN"));
        assertTrue(contains(controls, "SEND_MIX1"), "Missing SOF fader for MIX1");
    }


    @Test
    public void testFallbackMixToAuxWhenOnlyAuxExists() {
        MockCanonicalRegistry registry = new MockCanonicalRegistry();

        ControlGroup on = makeGroup("kInputOn", "kChannelOn", 1);
        ControlGroup pan = makeGroup("kInputPan", "kChannelPan", 1);

        ControlGroup auxSends = makeGroupMulti("kInputAUX", "kAUX1Level");

        registry.mapContext("channel.0", on, pan, auxSends);

        InputChannelSendsOnFaderViewBuilder builder = new InputChannelSendsOnFaderViewBuilder();
        List<ViewControl> controls = builder.build(channel0(), registry, "mix1");

        assertTrue(contains(controls, "SEND_AUX1"), "Expected MIX→AUX fallback to produce SEND_AUX1");
        assertFalse(contains(controls, "SEND_MIX1"), "Should not produce SEND_MIX1 when only AUX exists");
    }

    @Test
    public void testFallbackAuxToMixWhenOnlyMixExists() {
        MockCanonicalRegistry registry = new MockCanonicalRegistry();

        ControlGroup on = makeGroup("kInputOn", "kChannelOn", 1);
        ControlGroup pan = makeGroup("kInputPan", "kChannelPan", 1);

        ControlGroup mixSends = makeGroupMulti("kInputToMix", "kMix1Level");

        registry.mapContext("channel.0", on, pan, mixSends);

        InputChannelSendsOnFaderViewBuilder builder = new InputChannelSendsOnFaderViewBuilder();
        List<ViewControl> controls = builder.build(channel0(), registry, "aux1");

        assertTrue(contains(controls, "SEND_MIX1"), "Expected AUX→MIX fallback to produce SEND_MIX1");
        assertFalse(contains(controls, "SEND_AUX1"), "Should not produce SEND_AUX1 when only MIX exists");
    }

    @Test
    public void testBlankSuffixUsesDefaultAndStillFallsBack() {
        MockCanonicalRegistry registry = new MockCanonicalRegistry();

        ControlGroup on = makeGroup("kInputOn", "kChannelOn", 1);
        ControlGroup pan = makeGroup("kInputPan", "kChannelPan", 1);

        ControlGroup auxSends = makeGroupMulti("kInputAUX", "kAUX1Level");

        registry.mapContext("channel.0", on, pan, auxSends);

        InputChannelSendsOnFaderViewBuilder builder = new InputChannelSendsOnFaderViewBuilder();

        List<ViewControl> controls = builder.build(channel0(), registry, "   ");

        assertTrue(contains(controls, "SEND_AUX1"), "Expected default MIX1 to fall back to AUX1 when suffix blank");
    }

    @Test
    public void testZeroPaddedSendIndexMatchesMix1() {
        MockCanonicalRegistry registry = new MockCanonicalRegistry();

        ControlGroup on = makeGroup("kInputOn", "kChannelOn", 1);
        ControlGroup pan = makeGroup("kInputPan", "kChannelPan", 1);

        ControlGroup mixSends = makeGroupMulti("kInputToMix", "kMix01Level");

        registry.mapContext("channel.0", on, pan, mixSends);

        InputChannelSendsOnFaderViewBuilder builder = new InputChannelSendsOnFaderViewBuilder();
        List<ViewControl> controls = builder.build(channel0(), registry, "mix1");

        assertTrue(contains(controls, "SEND_MIX1"), "kMix01Level should match target mix1 (MIX1)");
    }

    @Test
    public void testNoMatchingSendFoundTriggersElseBranch() {
        MockCanonicalRegistry registry = new MockCanonicalRegistry();

        ControlGroup on = makeGroup("kInputOn", "kChannelOn", 1);
        ControlGroup pan = makeGroup("kInputPan", "kChannelPan", 1);

        ControlGroup mixSends = makeGroupMulti("kInputToMix", "kMix2Level");

        registry.mapContext("channel.0", on, pan, mixSends);

        InputChannelSendsOnFaderViewBuilder builder = new InputChannelSendsOnFaderViewBuilder();
        List<ViewControl> controls = builder.build(channel0(), registry, "mix1");

        assertFalse(contains(controls, "SEND_MIX1"), "No matching send exists, so SEND_MIX1 should not be present");
        assertTrue(contains(controls, "CHANNEL_ON"));
        assertTrue(contains(controls, "PAN"));
    }

    private boolean contains(List<ViewControl> list, String logicalId) {
        return list.stream().anyMatch(c -> logicalId.equals(c.getLogicalId()));
    }
}