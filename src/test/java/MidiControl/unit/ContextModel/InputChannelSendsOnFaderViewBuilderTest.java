
package MidiControl.unit.ContextModel;

import MidiControl.ContextModel.*;
import MidiControl.Controls.*;
import MidiControl.Mocks.MockCanonicalRegistry;
import MidiControl.SysexUtils.SysexMapping;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

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

    private SysexMapping dummyMapping(String group, String sub, int min, int max) {
        return new SysexMapping(
                group,
                0,
                1,
                sub,
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

        Context ctx = new Context(
                "channel.0",
                "Channel 1",
                ContextType.CHANNEL,
                List.of(),
                List.of()
        );

        InputChannelSendsOnFaderViewBuilder builder = new InputChannelSendsOnFaderViewBuilder();
        List<ViewControl> controls = builder.build(ctx, registry, "mix1");

        assertTrue(contains(controls, "CHANNEL_ON"));
        assertTrue(contains(controls, "PAN"));
        assertTrue(contains(controls, "SEND_MIX1"), "Missing SOF fader for MIX1");
    }

    private boolean contains(List<ViewControl> list, String logicalId) {
        return list.stream().anyMatch(c -> logicalId.equals(c.getLogicalId()));
    }
}
