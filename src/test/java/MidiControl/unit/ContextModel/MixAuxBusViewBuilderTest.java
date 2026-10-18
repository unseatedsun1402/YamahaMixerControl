package MidiControl.unit.ContextModel;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import MidiControl.ContextModel.Context;
import MidiControl.ContextModel.ContextFilter;
import MidiControl.ContextModel.ContextType;
import MidiControl.ContextModel.ViewControl;
import MidiControl.ContextModel.ViewBuilders.MixAuxBusViewBuilder;
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

        String[] bands = {"kEQ1", "kEQ2", "kEQ3", "kEQ4"};
        for (String band : bands) {
            for (String parameter : List.of("G", "F", "Q")) {
                String subcontrol = band + parameter;
                SubControl sub = new SubControl(eq, subcontrol);
                sub.addInstance(new ControlInstance(
                        sub,
                        0,
                        dummyMapping("kMixEQ", subcontrol, 0, 127),
                        null));
                eq.getSubcontrols().put(subcontrol, sub);
            }
        }

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

        assertEquals(12, controls.stream()
                .filter(c -> "input.eq".equals(c.uiGroup))
                .count());
        assertEquals(Set.of(
                        "kMixEQ.kEQ1G.0", "kMixEQ.kEQ1F.0", "kMixEQ.kEQ1Q.0",
                        "kMixEQ.kEQ2G.0", "kMixEQ.kEQ2F.0", "kMixEQ.kEQ2Q.0",
                        "kMixEQ.kEQ3G.0", "kMixEQ.kEQ3F.0", "kMixEQ.kEQ3Q.0",
                        "kMixEQ.kEQ4G.0", "kMixEQ.kEQ4F.0", "kMixEQ.kEQ4Q.0"),
                controls.stream()
                        .filter(c -> "input.eq".equals(c.uiGroup))
                        .map(c -> c.canonicalId)
                        .collect(Collectors.toSet()));
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
