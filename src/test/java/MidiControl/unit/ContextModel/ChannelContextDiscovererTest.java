package MidiControl.unit.ContextModel;

import MidiControl.ContextModel.*;
import MidiControl.Controls.*;
import MidiControl.Mocks.MockCanonicalRegistry;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ChannelContextDiscovererTest {

    private CanonicalRegistry buildMockM7Registry(int channelCount) {
        MockCanonicalRegistry registry = new MockCanonicalRegistry();

        ControlGroup faderGroup = new ControlGroup("kInputFader");
        SubControl faderSub = new SubControl(faderGroup, "kFader");

        for (int i = 0; i < channelCount; i++) {
            faderSub.addInstance(new ControlInstance(faderSub, i, null, null));
        }

        faderGroup.getSubcontrols().put("kFader", faderSub);
        registry.getGroups().put("kInputFader", faderGroup);


        ControlGroup mixGroup = new ControlGroup("kInputToMix");
        SubControl mixSub = new SubControl(mixGroup, "kMix1Level");

        for (int i = 0; i < channelCount; i++) {
            mixSub.addInstance(new ControlInstance(mixSub, i, null, null));
        }

        mixGroup.getSubcontrols().put("kMix1Level", mixSub);
        registry.getGroups().put("kInputToMix", mixGroup);

        return registry;
    }

        private CanonicalRegistry buildMock01VRegistry(int channelCount) {
        MockCanonicalRegistry registry = new MockCanonicalRegistry();

        ControlGroup faderGroup = new ControlGroup("kInputFader");
        SubControl faderSub = new SubControl(faderGroup, "kFader");

        for (int i = 0; i < channelCount; i++) {
            faderSub.addInstance(new ControlInstance(faderSub, i, null, null));
        }

        faderGroup.getSubcontrols().put("kFader", faderSub);
        registry.getGroups().put("kInputFader", faderGroup);

        ControlGroup auxGroup = new ControlGroup("kInputAUX");
        SubControl auxSub = new SubControl(auxGroup, "kAUX1Level");

        for (int i = 0; i < channelCount; i++) {
            auxSub.addInstance(new ControlInstance(auxSub, i, null, null));
        }

        auxGroup.getSubcontrols().put("kAUX1Level", auxSub);
        registry.getGroups().put("kInputAUX", auxGroup);

        return registry;
    }

    @Test
    public void test01V96iChannelDiscovery() {
        int expectedChannels = 40;
        CanonicalRegistry registry = buildMock01VRegistry(expectedChannels);

        ChannelContextDiscoverer discoverer = new ChannelContextDiscoverer();
        List<Context> out = new ArrayList<>();

        discoverer.discover(out, registry);

        assertEquals(expectedChannels, out.size(), "01V96i should have 40 channels");

        for (int i = 0; i < expectedChannels; i++) {
            Context ctx = out.get(i);

            assertEquals("channel." + i, ctx.getId());
            assertEquals("Channel " + (i + 1), ctx.getLabel());
            assertEquals(ContextType.CHANNEL, ctx.getContextType());

            // Filters should include both per-channel groups
            assertEquals(2, ctx.getFilters().size());
            assertTrue(ctx.getFilters().stream()
                    .anyMatch(f -> f.getControlGroup().equals("kInputFader")));
            assertTrue(ctx.getFilters().stream()
                    .anyMatch(f -> f.getControlGroup().equals("kInputAUX")));
        }
    }

    @Test
    public void testM7CLChannelDiscovery() {
        int expectedChannels = 56;
        CanonicalRegistry registry = buildMockM7Registry(expectedChannels);

        ChannelContextDiscoverer discoverer = new ChannelContextDiscoverer();
        List<Context> out = new ArrayList<>();

        discoverer.discover(out, registry);

        assertEquals(expectedChannels, out.size(), "M7CL should have 56 channels");

        for (int i = 0; i < expectedChannels; i++) {
            Context ctx = out.get(i);

            assertEquals("channel." + i, ctx.getId());
            assertEquals("Channel " + (i + 1), ctx.getLabel());
            assertEquals(ContextType.CHANNEL, ctx.getContextType());

            assertEquals(2, ctx.getFilters().size());
            assertTrue(ctx.getFilters().stream()
                    .anyMatch(f -> f.getControlGroup().equals("kInputFader")));
            assertTrue(ctx.getFilters().stream()
                    .anyMatch(f -> f.getControlGroup().equals("kInputToMix")));
        }
    }
}