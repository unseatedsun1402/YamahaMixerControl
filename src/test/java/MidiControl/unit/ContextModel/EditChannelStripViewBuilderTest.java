package MidiControl.unit.ContextModel;

import MidiControl.ContextModel.*;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.ControlInstance;
import MidiControl.Controls.SubControl;
import MidiControl.Mocks.MockCanonicalRegistry;
import MidiControl.SysexUtils.SysexMapping;

import org.junit.jupiter.api.Test;

import java.util.List;

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
                "test",
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
                builder.buildCompact(channelContext(0), registry);

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
                builder.buildCompact(invalid, registry);

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
                builder.buildCompact(invalid, registry);

        assertTrue(controls.isEmpty());
    }
}
