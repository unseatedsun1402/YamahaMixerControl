package MidiControl.unit.ContextModel;

import MidiControl.ContextModel.*;
import MidiControl.Controls.*;
import MidiControl.Mocks.MockCanonicalRegistry;
import MidiControl.SysexUtils.SysexMapping;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ContextFactoryTest {

    private SysexMapping dummyMap(String group, String sub, int min, int max) {
        return new SysexMapping(
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
    }

    private ControlGroup makeGroup(String group, String... subcontrols) {
        ControlGroup g = new ControlGroup(group);

        for (String sub : subcontrols) {
            SubControl sc = new SubControl(g, sub);
            ControlInstance ci = new ControlInstance(sc, 0, dummyMap(group, sub, 0, 127), null);
            sc.addInstance(ci);
            g.getSubcontrols().put(sub, sc);
        }

        return g;
    }

    private MockCanonicalRegistry registry(ControlGroup... groups) {
        MockCanonicalRegistry reg = new MockCanonicalRegistry();
        for (ControlGroup g : groups) {
            reg.getGroups().put(g.getName(), g);
        }
        return reg;
    }

    private boolean containsFilter(List<ContextFilter> filters,
                                   String group,
                                   String prefix,
                                   Integer index) {
        return filters.stream().anyMatch(f ->
                f.getControlGroup().equals(group) &&
                f.getSubControl().equals(prefix) &&
                Objects.equals(f.getIndex(), index)
        );
    }

    @Test
    public void testBuildChannelStrip_Unit() {

        ControlGroup fader = makeGroup("kFader", "kFader");
        ControlGroup pan   = makeGroup("kPan", "kPan");

        // Indexed EQ: kEQ1Gain, kEQ2Gain, kEQ3Gain
        ControlGroup eq = makeGroup("kEQ", "kEQ1Gain", "kEQ2Gain", "kEQ3Gain");

        // Indexed Mix sends
        ControlGroup mix = makeGroup("kMix", "kMix1Level", "kMix2Level");

        // Indexed AUX sends
        ControlGroup aux = makeGroup("kAUX", "kAUX1Level");

        MockCanonicalRegistry reg = registry(fader, pan, eq, mix, aux);

        Context ctx = ContextFactory.buildChannelStrip(reg, 5);

        assertEquals("channel.5", ctx.getId());
        assertEquals(ContextType.CHANNEL, ctx.getContextType());

        List<ContextFilter> filters = ctx.getFilters();

        // Single filters
        assertTrue(containsFilter(filters, "kFader", "kFader", null));
        assertTrue(containsFilter(filters, "kPan", "kPan", null));

        // Indexed EQ
        assertTrue(containsFilter(filters, "kEQ", "kEQ", 1));
        assertTrue(containsFilter(filters, "kEQ", "kEQ", 2));
        assertTrue(containsFilter(filters, "kEQ", "kEQ", 3));

        // Indexed Mix
        assertTrue(containsFilter(filters, "kMix", "kMix", 1));
        assertTrue(containsFilter(filters, "kMix", "kMix", 2));

        // Indexed AUX
        assertTrue(containsFilter(filters, "kAUX", "kAUX", 1));
    }

    @Test
    public void testBuildMixStrip_Unit() {

        ControlGroup fader = makeGroup("kMixFader", "kMixFader");
        ControlGroup nameS = makeGroup("kMixNameShort", "kMixNameShort");
        ControlGroup nameL = makeGroup("kMixNameLong", "kMixNameLong");

        MockCanonicalRegistry reg = registry(fader, nameS, nameL);

        Context ctx = ContextFactory.buildMixStrip(reg, 2);

        assertEquals("mix.2", ctx.getId());
        assertEquals(ContextType.MIX, ctx.getContextType());

        List<ContextFilter> filters = ctx.getFilters();

        assertTrue(containsFilter(filters, "kMixFader", "kMixFader", null));
        assertTrue(containsFilter(filters, "kMixNameShort", "kMixNameShort", null));
        assertTrue(containsFilter(filters, "kMixNameLong", "kMixNameLong", null));
    }

    @Test
    public void testBuildMatrixStrip_Unit() {

        ControlGroup fader = makeGroup("kMatrixFader", "kMatrixFader");
        ControlGroup nameS = makeGroup("kMatrixNameShort", "kMatrixNameShort");
        ControlGroup nameL = makeGroup("kMatrixNameLong", "kMatrixNameLong");

        ControlGroup send = makeGroup("kMatrixSend", "kMatrixSend1", "kMatrixSend2");
        ControlGroup eq   = makeGroup("kMatrixEQ", "kMatrixEQ1", "kMatrixEQ2", "kMatrixEQ3");

        MockCanonicalRegistry reg = registry(fader, nameS, nameL, send, eq);

        Context ctx = ContextFactory.buildMatrixStrip(reg, 1);

        assertEquals("matrix.1", ctx.getId());
        assertEquals(ContextType.MATRIX, ctx.getContextType());

        List<ContextFilter> filters = ctx.getFilters();

        assertTrue(containsFilter(filters, "kMatrixFader", "kMatrixFader", null));
        assertTrue(containsFilter(filters, "kMatrixNameShort", "kMatrixNameShort", null));
        assertTrue(containsFilter(filters, "kMatrixNameLong", "kMatrixNameLong", null));

        assertTrue(containsFilter(filters, "kMatrixSend", "kMatrixSend", 1));
        assertTrue(containsFilter(filters, "kMatrixSend", "kMatrixSend", 2));

        assertTrue(containsFilter(filters, "kMatrixEQ", "kMatrixEQ", 1));
        assertTrue(containsFilter(filters, "kMatrixEQ", "kMatrixEQ", 2));
        assertTrue(containsFilter(filters, "kMatrixEQ", "kMatrixEQ", 3));
    }


    @Test
    public void testBuildDcaStrip_Unit() {

        ControlGroup fader = makeGroup("kDCAFader", "kDCAFader");
        ControlGroup nameS = makeGroup("kDCANameShort", "kDCANameShort");
        ControlGroup nameL = makeGroup("kDCANameLong", "kDCANameLong");

        MockCanonicalRegistry reg = registry(fader, nameS, nameL);

        Context ctx = ContextFactory.buildDcaStrip(reg, 4);

        assertEquals("dca.4", ctx.getId());
        assertEquals(ContextType.DCA, ctx.getContextType());

        List<ContextFilter> filters = ctx.getFilters();

        assertTrue(containsFilter(filters, "kDCAFader", "kDCAFader", null));
        assertTrue(containsFilter(filters, "kDCANameShort", "kDCANameShort", null));
        assertTrue(containsFilter(filters, "kDCANameLong", "kDCANameLong", null));
    }

    @Test
    public void testBuildStereoInput_Unit() {

        ControlGroup fader = makeGroup("kStereoFader", "kStereoFader");
        ControlGroup pan   = makeGroup("kStereoPan", "kStereoPan");
        ControlGroup nameS = makeGroup("kStereoNameShort", "kStereoNameShort");
        ControlGroup nameL = makeGroup("kStereoNameLong", "kStereoNameLong");

        ControlGroup eq   = makeGroup("kStereoEQ", "kStereoEQ1", "kStereoEQ2");
        ControlGroup send = makeGroup("kStereoSend", "kStereoSend1", "kStereoSend2", "kStereoSend3");

        MockCanonicalRegistry reg = registry(fader, pan, nameS, nameL, eq, send);

        Context ctx = ContextFactory.buildStereoInput(reg, 1);

        assertEquals("stereo.1", ctx.getId());
        assertEquals(ContextType.STEREO_OUTPUT, ctx.getContextType());

        List<ContextFilter> filters = ctx.getFilters();

        assertTrue(containsFilter(filters, "kStereoFader", "kStereoFader", null));
        assertTrue(containsFilter(filters, "kStereoPan", "kStereoPan", null));

        assertTrue(containsFilter(filters, "kStereoEQ", "kStereoEQ", 1));
        assertTrue(containsFilter(filters, "kStereoEQ", "kStereoEQ", 2));

        assertTrue(containsFilter(filters, "kStereoSend", "kStereoSend", 1));
        assertTrue(containsFilter(filters, "kStereoSend", "kStereoSend", 2));
        assertTrue(containsFilter(filters, "kStereoSend", "kStereoSend", 3));
    }

    @Test
    public void testFromId_Unit() {
        MockCanonicalRegistry reg = registry();

        assertNotNull(ContextFactory.fromId("channel.1", reg));
        assertNotNull(ContextFactory.fromId("mix.2", reg));
        assertNotNull(ContextFactory.fromId("matrix.3", reg));
        assertNotNull(ContextFactory.fromId("dca.4", reg));
        assertNotNull(ContextFactory.fromId("stereo.5", reg));

        assertNull(ContextFactory.fromId("unknown.1", reg));
        assertNull(ContextFactory.fromId("badformat", reg));
        assertNull(ContextFactory.fromId(null, reg));
    }
}