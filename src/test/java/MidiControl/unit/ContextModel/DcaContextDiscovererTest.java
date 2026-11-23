package MidiControl.unit.ContextModel;

import static org.junit.jupiter.api.Assertions.*;

import MidiControl.ContextModel.*;
import MidiControl.Controls.*;
import MidiControl.Mocks.MockCanonicalRegistry;
import java.util.*;
import org.junit.jupiter.api.Test;

public class DcaContextDiscovererTest {

  private CanonicalRegistry buildMockM7DcaRegistry() {

    MockCanonicalRegistry registry = new MockCanonicalRegistry();

    ControlGroup group = new ControlGroup("kDCAFader");

    SubControl fader = new SubControl(group, "kFader");

    for (int i = 0; i < 8; i++) {
      fader.addInstance(new ControlInstance(fader, i, null, null));
    }

    group.getSubcontrols().put("kFader", fader);

    registry.getGroups().put("kDCAFader", group);

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
  public void test01V96iHasNoDcas() {

    CanonicalRegistry registry = buildMock01VRegistry(40);

    DcaContextDiscoverer discoverer = new DcaContextDiscoverer();

    List<Context> out = new ArrayList<>();

    discoverer.discover(out, registry);

    assertTrue(out.isEmpty());
  }

  @Test
  public void testM7CLDcaDiscovery() {

    CanonicalRegistry registry = buildMockM7DcaRegistry();

    DcaContextDiscoverer discoverer = new DcaContextDiscoverer();

    List<Context> out = new ArrayList<>();

    discoverer.discover(out, registry);

    assertEquals(8, out.size());

    for (int i = 0; i < 8; i++) {

      final int index = i;

      Context ctx = out.get(i);

      assertTrue(
          ctx.getFilters().stream()
              .anyMatch(f -> f.getControlGroup().equals("kDCAFader") && f.getIndex() == index));
    }
  }

  @Test
  public void testDcaContextsAreSequential() {

    CanonicalRegistry registry = buildMockM7DcaRegistry();

    DcaContextDiscoverer discoverer = new DcaContextDiscoverer();

    List<Context> out = new ArrayList<>();

    discoverer.discover(out, registry);

    List<String> ids = out.stream().map(Context::getId).toList();

    assertEquals(
        List.of("dca.0", "dca.1", "dca.2", "dca.3", "dca.4", "dca.5", "dca.6", "dca.7"), ids);
  }
}
