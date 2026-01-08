package MidiControl.unit.ContextModel;

import MidiControl.ContextModel.*;
import MidiControl.Controls.*;
import MidiControl.Mocks.MockCanonicalRegistry;
import MidiControl.SysexUtils.SysexMapping;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

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
                0,
                0L,
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
    public void testMixAuxBusViewBuilder() {

        MockCanonicalRegistry registry = new MockCanonicalRegistry();

        // Core controls
        ControlGroup fader = makeGroup("kMixFader", "kFader", 1);
        ControlGroup pan = makeGroup("kMixPan", "kPan", 1);

        // Dynamics
        ControlGroup dynThresh = makeGroup("kMixDyn", "kThreshold", 1);
        ControlGroup dynRatio = makeGroup("kMixDyn", "kRatio", 1);

        // EQ (gain only)
        ControlGroup eqLow = makeGroup("kMixEQ", "kLowGain", 1);
        ControlGroup eqHigh = makeGroup("kMixEQ", "kHighGain", 1);

        // Add groups to registry
        registry.getGroups().put("kMixFader", fader);
        registry.getGroups().put("kMixPan", pan);
        registry.getGroups().put("kMixDyn", dynThresh);
        registry.getGroups().put("kMixDyn", dynRatio);
        registry.getGroups().put("kMixEQ", eqLow);
        registry.getGroups().put("kMixEQ", eqHigh);

        // Map context → instances
        registry.mapContext("mix.0", fader, pan, dynThresh, dynRatio, eqLow, eqHigh);

        Context ctx = new Context(
                "mix.0",
                "Mix 1",
                ContextType.MIX,
                List.of("Monitor"),
                List.of(new ContextFilter("kMixFader", "*", 0))
        );

        MixAuxBusViewBuilder builder = new MixAuxBusViewBuilder(registry);
        List<ViewControl> controls = builder.build(ctx, registry);

        // Validate presence
        assertTrue(contains(controls, "FADER"));
        assertTrue(contains(controls, "PAN"));
        assertTrue(containsPrefix(controls, "DYN_"));
        assertTrue(containsPrefix(controls, "EQ_"));

        // Validate ordering: Fader → Pan → Dyn → EQ
        int idxFader = indexOf(controls, "FADER");
        int idxPan = indexOf(controls, "PAN");
        int idxDyn = firstIndexStartingWith(controls, "DYN_");
        int idxEQ = firstIndexStartingWith(controls, "EQ_");

        assertTrue(idxFader < idxPan);
        assertTrue(idxPan < idxDyn);
        assertTrue(idxDyn < idxEQ);
    }

    private boolean contains(List<ViewControl> list, String logicalId) {
        return list.stream().anyMatch(c -> logicalId.equals(c.getLogicalId()));
    }

    private boolean containsPrefix(List<ViewControl> list, String prefix) {
        return list.stream().anyMatch(c -> c.getLogicalId().startsWith(prefix));
    }

    private int indexOf(List<ViewControl> list, String logicalId) {
        for (int i = 0; i < list.size(); i++) {
            if (logicalId.equals(list.get(i).getLogicalId())) {
                return i;
            }
        }
        return -1;
    }

    private int firstIndexStartingWith(List<ViewControl> list, String prefix) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getLogicalId().startsWith(prefix)) {
                return i;
            }
        }
        return -1;
    }
}
