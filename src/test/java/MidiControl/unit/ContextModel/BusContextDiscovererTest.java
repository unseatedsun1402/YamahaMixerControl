package MidiControl.unit.ContextModel;

import MidiControl.ContextModel.*;
import MidiControl.Controls.*;
import MidiControl.Mocks.MockCanonicalRegistry;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class BusContextDiscovererTest {

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private ControlGroup makeGroup(String name, String subName, int instances) {
        ControlGroup g = new ControlGroup(name);
        SubControl sub = new SubControl(g, subName);

        for (int i = 0; i < instances; i++) {
            sub.addInstance(new ControlInstance(sub, i, null, null));
        }

        g.getSubcontrols().put(subName, sub);
        return g;
    }

    private CanonicalRegistry registryOf(ControlGroup... groups) {
        MockCanonicalRegistry reg = new MockCanonicalRegistry();
        for (ControlGroup g : groups) {
            reg.getGroups().put(g.getName(), g);
        }
        return reg;
    }

    // ------------------------------------------------------------
    // 01V96i: AUX (8), BUS (8), STEREO (2)
    // ------------------------------------------------------------
    @Test
    public void test01V96iBusDiscovery() {
        ControlGroup auxFader = makeGroup("kAUXFader", "kFader", 8);
        ControlGroup busFader = makeGroup("kBusFader", "kFader", 8);
        ControlGroup stereoFader = makeGroup("kStereoFader", "kFader", 2);

        CanonicalRegistry registry = registryOf(auxFader, busFader, stereoFader);

        BusContextDiscoverer discoverer = new BusContextDiscoverer();
        List<Context> out = new ArrayList<>();

        discoverer.discover(out, registry);

        // Expect:
        // aux.0..7 (8)
        // bus.0..7 (8)
        // stereo.0/1 (2)
        assertEquals(18, out.size());

        // AUX
        for (int i = 0; i < 8; i++) {
            final int index = i;
            Context ctx = out.stream()
                .filter(c -> c.getId().equals("aux." + index))
                .findFirst()
                .orElseThrow();

            assertEquals("Aux " + (index + 1), ctx.getLabel());
        }

        // BUS
        for (int i = 0; i < 8; i++) {
            final int index = i;
            Context ctx = out.stream()
                .filter(c -> c.getId().equals("bus." + index))
                .findFirst()
                .orElseThrow();

            assertEquals("Bus " + (index + 1), ctx.getLabel());
        }

        // STEREO
        Context stereo = out.stream().filter(c -> c.getId().equals("stereo.0")).findFirst().orElseThrow();
        assertEquals("Stereo 1", stereo.getLabel());
    }

    // ------------------------------------------------------------
    // M7CL: MIX (16), MATRIX (8), STEREO (3)
    // ------------------------------------------------------------
    @Test
    public void testM7CLBusDiscovery() {
        ControlGroup mixFader = makeGroup("kMixFader", "kFader", 16);
        ControlGroup matrixFader = makeGroup("kMatrixFader", "kFader", 8);
        ControlGroup stereoFader = makeGroup("kStereoFader", "kFader", 3);

        CanonicalRegistry registry = registryOf(mixFader, matrixFader, stereoFader);

        BusContextDiscoverer discoverer = new BusContextDiscoverer();
        List<Context> out = new ArrayList<>();

        discoverer.discover(out, registry);

        // Expect:
        // mix.0..15 (16)
        // matrix.0..7 (8)
        // stereo.0/1/2 (3)
        assertEquals(27, out.size());

        // MIX
        for (int i = 0; i < 8; i++) {
            final int index = i;
            Context ctx = out.stream()
                .filter(c -> c.getId().equals("mix." + index))
                .findFirst()
                .orElseThrow();

            assertEquals("Mix " + (index + 1), ctx.getLabel());
        }

        // MATRIX
        for (int i = 0; i < 8; i++) {
            final int index = i;
            Context ctx = out.stream()
                .filter(c -> c.getId().equals("matrix." + index))
                .findFirst()
                .orElseThrow();

            assertEquals("Matrix " + (index + 1), ctx.getLabel());
        }

        // STEREO
        Context stereo = out.stream().filter(c -> c.getId().equals("stereo.0")).findFirst().orElseThrow();
        assertEquals("Stereo 1", stereo.getLabel());
    }

    // ------------------------------------------------------------
    // Missing families should produce no contexts
    // ------------------------------------------------------------
    @Test
    public void testMissingFamiliesProducesNoContexts() {
        // No kMix*, no kAUX*, no kBus*, no kMatrix*, no kStereo* 
        CanonicalRegistry registry = new MockCanonicalRegistry();

        BusContextDiscoverer discoverer = new BusContextDiscoverer();
        List<Context> out = new ArrayList<>();

        discoverer.discover(out, registry);

        assertTrue(out.isEmpty(), "No bus families → no contexts");
    }
}