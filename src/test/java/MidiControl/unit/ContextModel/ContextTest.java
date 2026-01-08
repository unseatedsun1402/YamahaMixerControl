package MidiControl.unit.ContextModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import MidiControl.ContextModel.Context;
import MidiControl.ContextModel.ContextFilter;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.ControlInstance;
import MidiControl.Controls.SubControl;
import MidiControl.ContextModel.ContextResolver;
import MidiControl.ContextModel.ContextType;

public class ContextTest {
    private Map<String, ControlGroup> buildCanonicalTree() {
        ControlGroup group = new ControlGroup("kInput");

        SubControl fader = new SubControl(group, "Fader");

        ControlInstance i0 = new ControlInstance(fader, 0, null, null);
        ControlInstance i1 = new ControlInstance(fader, 1, null, null);
        ControlInstance i2 = new ControlInstance(fader, 2, null, null);

        fader.addInstance(i0);
        fader.addInstance(i1);
        fader.addInstance(i2);

        group.addSubcontrol(fader);

        return Map.of("kInput", group);
    }

    @Test
    void testRegisterAndRetrieveContext() {
        ContextResolver resolver = new ContextResolver(buildCanonicalTree());

        Context ctx = new Context(
            "channel.1",
            "Channel 1",
            ContextType.CHANNEL,
            List.of("FOH"),
            List.of(new ContextFilter("kInput", "Fader", 1))
        );

        resolver.registerContext(ctx);

        Optional<Context> result = resolver.getContext("channel.1");

        assertTrue(result.isPresent());
        assertEquals("channel.1", result.get().getId());
    }

    @Test
    void testResolveSingleControl() {
        Map<String, ControlGroup> tree = buildCanonicalTree();
        tree.forEach((name, group) -> {
            System.out.println("Group: " + name);
            for (SubControl sub : group.getSubcontrols().values()) {
                System.out.println("  Sub: " + sub.getName());
                for (ControlInstance inst : sub.getInstances()) {
                    System.out.println("    Inst: " + inst.getCanonicalId());
                }
            }
        });

        ContextResolver resolver = new ContextResolver(tree);

        resolver.registerContext(new Context(
            "channel.1",
            "Channel 1",
            ContextType.CHANNEL,
            List.of("FOH"),
            List.of(new ContextFilter("kInput", "Fader", 1))
        ));

        List<ControlInstance> result = resolver.resolve("channel.1");

        assertEquals(1, result.size());
        assertEquals("kInput.Fader.1", result.get(0).getCanonicalId());
    }

    @Test
    void testResolveWildcardSubControl() {
        ContextResolver resolver = new ContextResolver(buildCanonicalTree());

        resolver.registerContext(new Context(
            "all.faders",
            "All Faders",
            ContextType.CHANNEL,
            List.of("FOH"),
            List.of(new ContextFilter("kInput", "*", null))
        ));

        List<ControlInstance> result = resolver.resolve("all.faders");

        assertEquals(3, result.size());
    }

    @Test
    void testResolveMultipleFilters() {
        ContextResolver resolver = new ContextResolver(buildCanonicalTree());

        resolver.registerContext(new Context(
            "multi",
            "Multi Filter",
            ContextType.CHANNEL,
            List.of("FOH"),
            List.of(
                new ContextFilter("kInput", "Fader", 0),
                new ContextFilter("kInput", "Fader", 2)
            )
        ));

        List<ControlInstance> result = resolver.resolve("multi");

        assertEquals(2, result.size());
        assertEquals("kInput.Fader.0", result.get(0).getCanonicalId());
        assertEquals("kInput.Fader.2", result.get(1).getCanonicalId());
    }

    @Test
    void testResolveUnknownContextThrows() {
        ContextResolver resolver = new ContextResolver(buildCanonicalTree());

        assertThrows(IllegalArgumentException.class, () -> {
            resolver.resolve("does.not.exist");
        });
    }

    @Test
    void testUnknownControlGroupReturnsEmpty() {
        ContextResolver resolver = new ContextResolver(buildCanonicalTree());

        resolver.registerContext(new Context(
            "bad.group",
            "Bad Group",
            ContextType.CHANNEL,
            List.of("FOH"),
            List.of(new ContextFilter("kDoesNotExist", "*", null))
        ));

        List<ControlInstance> result = resolver.resolve("bad.group");

        assertTrue(result.isEmpty());
    }

    @Test
    void testUnknownSubControlReturnsEmpty() {
        ContextResolver resolver = new ContextResolver(buildCanonicalTree());

        resolver.registerContext(new Context(
            "bad.sub",
            "Bad Sub",
            ContextType.CHANNEL,
            List.of("FOH"),
            List.of(new ContextFilter("kInput", "NotARealSub", null))
        ));

        List<ControlInstance> result = resolver.resolve("bad.sub");

        assertTrue(result.isEmpty());
    }
}
