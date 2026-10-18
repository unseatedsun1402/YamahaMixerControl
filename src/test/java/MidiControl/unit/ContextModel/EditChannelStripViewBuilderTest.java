package MidiControl.unit.ContextModel;

import MidiControl.ContextModel.*;
import MidiControl.ContextModel.ViewBuilders.EditChannelStripViewBuilder;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.ControlInstance;
import MidiControl.Controls.SubControl;
import MidiControl.Controls.Widgets.EQ;
import MidiControl.Mocks.MockCanonicalRegistry;
import MidiControl.SysexUtils.SysexMapping;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class EditChannelStripViewBuilderTest {

    // -------------------------
    // Test helpers
    // -------------------------

    private ControlInstance makeControl(
            MockCanonicalRegistry registry,
            String groupName,
            String subName,
            int index,
            int min,
            int max,
            int value,
            int defaultValue
    ) {
        return makeControl(
                registry,
                groupName,
                subName,
                index,
                min,
                max,
                value,
                defaultValue,
                "test");
    }

    private ControlInstance makeControl(
            MockCanonicalRegistry registry,
            String groupName,
            String subName,
            int index,
            int min,
            int max,
            int value,
            int defaultValue,
            String comment
    ) {
        ControlGroup group = registry.getGroup(groupName);
        if (group == null) {
            group = new ControlGroup(groupName);
            registry.getGroups().put(groupName, group);
        }

        SubControl sub = group.getSubcontrol(subName);
        if (sub == null) {
            sub = new SubControl(group, subName);
            group.getSubcontrols().put(subName, sub);
        }

        SysexMapping mapping = new SysexMapping(
                groupName,
                0,
                1,
                subName,
                null,
                index,
                0L,
                new int[]{0},
                new int[]{0},
                min,
                min,
                max,
                defaultValue,
                comment,
                List.of("F0"),
                List.of("F0"),
                3
        );

        ControlInstance ci = new ControlInstance(sub, index, mapping, null);
        ci.setValue(value);

        while (sub.getInstances().size() <= index) {
            sub.getInstances().add(null);
        }
        sub.getInstances().set(index, ci);

        return ci;
    }

    private Context channelContext(int index) {
        return new Context(
                "channel." + index,
                "Channel " + index,
                ContextType.CHANNEL,
                List.of(),
                List.of()
        );
    }

    // -------------------------
    // Tests
    // -------------------------

    @Test
    void buildCompact_returnsOnlyChannelOn_whenPanIsMissing() {
        MockCanonicalRegistry registry = new MockCanonicalRegistry();

        makeControl(registry,
                "kInputOn", "kChannelOn",
                0, 0, 1, 0, 0);

        registry.mapContext("channel.0",
                registry.getGroup("kInputOn"));

        EditChannelStripViewBuilder builder = new EditChannelStripViewBuilder();

        List<ViewControl> controls =
                builder.buildCompact(channelContext(0), registry, null);

        assertEquals(1, controls.size());
        assertEquals("CHANNEL_ON", controls.get(0).logicId);
    }

    @Test
    void buildCompact_returnsEmptyList_whenContextIdHasNoIndex() {
        MockCanonicalRegistry registry = new MockCanonicalRegistry();

        Context invalid = new Context(
                "channel",
                "Channel",
                ContextType.CHANNEL,
                List.of(),
                List.of()
        );

        EditChannelStripViewBuilder builder = new EditChannelStripViewBuilder();

        List<ViewControl> controls =
                builder.buildCompact(invalid, registry, null);

        assertTrue(controls.isEmpty());
    }

    @Test
    void buildCompact_returnsEmptyList_whenContextIndexIsNotNumeric() {
        MockCanonicalRegistry registry = new MockCanonicalRegistry();

        Context invalid = new Context(
                "channel.X",
                "Channel X",
                ContextType.CHANNEL,
                List.of(),
                List.of()
        );

        EditChannelStripViewBuilder builder = new EditChannelStripViewBuilder();

        List<ViewControl> controls =
                builder.buildCompact(invalid, registry,null);

        assertTrue(controls.isEmpty());
    }

    @Test
    void buildCompact_includesAllFourEqBandsForPresetWidget() {
        MockCanonicalRegistry registry = new MockCanonicalRegistry();
        String[] bands = {"kEQ1", "kEQ2", "kEQ3", "kEQ4"};

        for (String band : bands) {
            for (String parameter : List.of("G", "F", "Q")) {
                makeControl(registry,
                        "kInputEQ", band + parameter,
                        0, -180, 180, 0, 0);
            }
        }

        registry.mapContext("channel.0", registry.getGroup("kInputEQ"));

        List<ViewControl> controls =
                new EditChannelStripViewBuilder().buildCompact(channelContext(0), registry, null);

        assertEquals(12, controls.size());
        assertEquals(4, controls.stream()
                .filter(control -> control.controlRole.endsWith("_GAIN"))
                .count());
        assertEquals(4, controls.stream()
                .filter(control -> control.controlRole.endsWith("_FREQUENCY"))
                .count());
        assertEquals(4, controls.stream()
                .filter(control -> control.controlRole.endsWith("_Q"))
                .count());
        assertEquals(Set.of(
                        "kInputEQ.kEQ1G.0", "kInputEQ.kEQ1F.0", "kInputEQ.kEQ1Q.0",
                        "kInputEQ.kEQ2G.0", "kInputEQ.kEQ2F.0", "kInputEQ.kEQ2Q.0",
                        "kInputEQ.kEQ3G.0", "kInputEQ.kEQ3F.0", "kInputEQ.kEQ3Q.0",
                        "kInputEQ.kEQ4G.0", "kInputEQ.kEQ4F.0", "kInputEQ.kEQ4Q.0"),
                controls.stream().map(control -> control.canonicalId).collect(Collectors.toSet()));
    }

    @Test
    void buildCompact_supportsLegacyEqSubcontrolNames() {
        MockCanonicalRegistry registry = new MockCanonicalRegistry();
        String[] bands = {"kEQLow", "kEQLowMid", "kEQHiMid", "kEQHi"};

        for (String band : bands) {
            for (String parameter : List.of("G", "F", "Q")) {
                makeControl(registry,
                        "kInputEQ", band + parameter,
                        0, -180, 180, 0, 0);
            }
        }

        registry.mapContext("channel.0", registry.getGroup("kInputEQ"));

        List<ViewControl> controls =
                new EditChannelStripViewBuilder().buildCompact(channelContext(0), registry, null);

        assertEquals(12, controls.size());
        assertEquals(Set.of("INPUT_EQ1_GAIN", "INPUT_EQ2_GAIN", "INPUT_EQ3_GAIN", "INPUT_EQ4_GAIN"),
                controls.stream()
                        .filter(control -> control.controlRole.endsWith("_GAIN"))
                        .map(control -> control.controlRole)
                        .collect(Collectors.toSet()));
        assertTrue(controls.stream().allMatch(control -> control.channelIndex == 0));
    }

    @Test
    void eqGainControl_exposesDeskSpecificDecibelRange() {
        MockCanonicalRegistry registry = new MockCanonicalRegistry();
        ControlInstance gain = makeControl(
                registry,
                "kInputEQ",
                "kEQ1G",
                0,
                -180,
                180,
                0,
                0,
                "-15dB - +15dB");

        ViewControl control = EQ.createEq1Gain(gain, "edit-channel-view", "channel.0");

        assertEquals("dB", control.unit);
        assertEquals(-15.0, control.unitMin);
        assertEquals(15.0, control.unitMax);
    }
}
