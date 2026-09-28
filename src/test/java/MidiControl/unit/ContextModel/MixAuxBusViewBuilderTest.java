package MidiControl.unit.ContextModel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import MidiControl.ContextModel.Context;
import MidiControl.ContextModel.ContextFilter;
import MidiControl.ContextModel.ContextType;
import MidiControl.ContextModel.MixAuxBusViewBuilder;
import MidiControl.ContextModel.ViewControl;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.ControlInstance;
import MidiControl.Controls.SubControl;
import MidiControl.Mocks.MockCanonicalRegistry;
import MidiControl.SysexUtils.SysexMapping;

public class MixAuxBusViewBuilderTest {

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

    private SysexMapping dummyMapping(String group, String sub, int min, int max) {
        SysexMapping m = new SysexMapping(
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
        return m;
    }

    @Test
    public void testCreatesFaderControl() {

        MockCanonicalRegistry registry = new MockCanonicalRegistry();

        ControlGroup group = makeGroup(
                "kMixFader",
                "kFader",
                1);

        registry.getGroups().put(
                "kMixFader",
                group);

        registry.mapContext(
                "mix.0",
                group);

        Context ctx = new Context(
                "mix.0",
                "Mix 1",
                ContextType.MIX,
                List.of(),
                List.of(
                        new ContextFilter(
                                "kMixFader",
                                "*",
                                0)));

        MixAuxBusViewBuilder builder =
                new MixAuxBusViewBuilder();

        List<ViewControl> controls =
                builder.build(ctx, registry, null);

        assertTrue(
                controls.stream()
                        .anyMatch(c ->
                                "FADER".equals(c.logicId)));
    }

    @Test
    public void testCreatesEQControls() {

        MockCanonicalRegistry registry =
                new MockCanonicalRegistry();

        ControlGroup eq =
                new ControlGroup("kMixEQ");

        SubControl eq1 =
                new SubControl(
                        eq,
                        "kEQ1G");

        eq1.addInstance(
                new ControlInstance(
                        eq1,
                        0,
                        dummyMapping(
                                "kMixEQ",
                                "kEQ1G",
                                0,
                                127),
                        null));

        SubControl eq2 =
                new SubControl(
                        eq,
                        "kEQ2G");

        eq2.addInstance(
                new ControlInstance(
                        eq2,
                        0,
                        dummyMapping(
                                "kMixEQ",
                                "kEQ2G",
                                0,
                                127),
                        null));

        eq.getSubcontrols().put(
                "kEQ1G",
                eq1);

        eq.getSubcontrols().put(
                "kEQ2G",
                eq2);

        registry.getGroups().put(
                "kMixEQ",
                eq);

        registry.mapContext(
                "mix.0",
                eq);

        Context ctx = new Context(
                "mix.0",
                "Mix 1",
                ContextType.MIX,
                List.of(),
                List.of(
                        new ContextFilter(
                                "kMixEQ",
                                "*",
                                0)));

        List<ViewControl> controls =
                new MixAuxBusViewBuilder()
                        .build(
                                ctx,
                                registry,
                                null);

        assertTrue(
                controls.stream()
                        .anyMatch(c ->
                                "EQ1G".equals(c.logicId)));

        assertTrue(
                controls.stream()
                        .anyMatch(c ->
                                "EQ2G".equals(c.logicId)));
    }    

    private boolean contains(List<ViewControl> list, String logicalId) {
        return list.stream().anyMatch(c -> logicalId.equals(c.logicId));
    }

    private boolean containsPrefix(List<ViewControl> list, String prefix) {
        return list.stream().anyMatch(c -> c.logicId.startsWith(prefix));
    }

    private int indexOf(List<ViewControl> list, String logicalId) {
        for (int i = 0; i < list.size(); i++) {
            if (logicalId.equals(list.get(i).logicId)) {
                return i;
            }
        }
        return -1;
    }

    private int firstIndexStartingWith(List<ViewControl> list, String prefix) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).logicId.startsWith(prefix)) {
                return i;
            }
        }
        return -1;
    }

    
}
