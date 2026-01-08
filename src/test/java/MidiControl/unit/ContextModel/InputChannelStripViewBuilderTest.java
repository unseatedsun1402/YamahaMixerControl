package MidiControl.unit.ContextModel;

import MidiControl.ContextModel.*;
import MidiControl.Controls.*;
import MidiControl.Mocks.MockCanonicalRegistry;
import MidiControl.SysexUtils.SysexMapping;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class InputChannelStripViewBuilderTest {

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
                group,              // control_group
                0,                  // control_id
                1,                  // max_channels
                sub,                // sub_control
                0,                  // channel_index
                0L,                 // key
                min,                // value
                min,                // min_value
                max,                // max_value
                min,                // default_value
                "test",             // comment
                List.of("F0", "00"), // parameter_change_format (dummy but valid)
                List.of("F0", "00"),  // parameter_request_format (dummy but valid)
                3
        );
        return m;
    }

    @Test
    public void testInputChannelStripBuilder() {

        MockCanonicalRegistry registry = new MockCanonicalRegistry();

        ControlGroup on = makeGroup("kInputOn", "kChannelOn", 1);
        ControlGroup pan = makeGroup("kInputPan", "kChannelPan", 1);
        ControlGroup fader = makeGroup("kInputFader", "kFader", 1);

        ControlGroup send1 = makeGroup("kInputToMix", "kMix1Level", 1);
        ControlGroup send2 = makeGroup("kInputToMix", "kMix2Level", 1);

        // Add groups to registry
        registry.getGroups().put("kInputOn", on);
        registry.getGroups().put("kInputPan", pan);
        registry.getGroups().put("kInputFader", fader);
        registry.getGroups().put("kInputToMix", send1);
        registry.getGroups().put("kInputToMix", send2);

        // Map context → instances
        registry.mapContext("channel.0", on, pan, fader, send1, send2);

        Context ctx = new Context(
                "channel.0",
                "Channel 1",
                ContextType.CHANNEL,
                List.of("Musician"),
                List.of(new ContextFilter("kInputFader", "*", 0))
        );

        InputChannelStripViewBuilder builder = new InputChannelStripViewBuilder(registry);
        List<ViewControl> controls = builder.build(ctx, registry);

        assertTrue(contains(controls, "CHANNEL_ON"));
        assertTrue(contains(controls, "PAN"));
        assertTrue(contains(controls, "SEND_MIX1"));
        assertTrue(contains(controls, "SEND_MIX2"));
        assertTrue(contains(controls, "FADER"));
    }

    private boolean contains(List<ViewControl> list, String logicalId) {
        return list.stream().anyMatch(c -> logicalId.equals(c.getLogicalId()));
    }
}