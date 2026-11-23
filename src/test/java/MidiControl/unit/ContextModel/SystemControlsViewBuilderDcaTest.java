package MidiControl.unit.ContextModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import MidiControl.ContextModel.Context;
import MidiControl.ContextModel.ContextFilter;
import MidiControl.ContextModel.ContextType;
import MidiControl.ContextModel.ViewBuilders.SystemControlsViewBuilder;
import MidiControl.ContextModel.ViewControl;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.ControlInstance;
import MidiControl.Controls.SubControl;
import MidiControl.Mocks.MockCanonicalRegistry;
import MidiControl.SysexUtils.SysexMapping;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SystemControlsViewBuilderDcaTest {
  private ControlGroup makeGroup(String group, String subcontrol, int instances) {
    ControlGroup g = new ControlGroup(group);
    SubControl sc = new SubControl(g, subcontrol);

    for (int i = 0; i < instances; i++) {
      SysexMapping map = dummyMapping(group, subcontrol, 0, 127);

      ControlInstance ci = new ControlInstance(sc, i, map, null);

      sc.addInstance(ci);
    }

    g.getSubcontrols().put(subcontrol, sc);

    return g;
  }

  private Context dca0() {

    List<ContextFilter> filters = new ArrayList<>();

    filters.add(new ContextFilter("kDCAFader", "kFader", 0));
    filters.add(new ContextFilter("kDCAMute", "kDCAMuteOn", 0));
    filters.add(new ContextFilter("kDCAName", "kNameShort1", 0));
    filters.add(new ContextFilter("kDCAName", "kNameShort2", 0));

    return new Context("dca.0", "DCA 1", ContextType.DCA, List.of(), filters);
  }

  private ControlGroup makeGroupMulti(String group, String... subcontrols) {

    ControlGroup g = new ControlGroup(group);

    for (String sub : subcontrols) {

      SubControl sc = new SubControl(g, sub);

      SysexMapping map = dummyMapping(group, sub, 0, 127);

      ControlInstance ci = new ControlInstance(sc, 0, map, null);

      sc.addInstance(ci);

      g.getSubcontrols().put(sub, sc);
    }

    return g;
  }

  private SysexMapping dummyMapping(String group, String sub, int min, int max) {

    return new SysexMapping(
        group,
        0,
        1,
        sub,
        null,
        0,
        0L,
        new int[] {0},
        new int[] {0},
        min,
        min,
        max,
        min,
        "test",
        List.of("F0", "00"),
        List.of("F0", "00"),
        3);
  }

  @Test
  public void testCreatesDcaFader() {
    MockCanonicalRegistry registry = new MockCanonicalRegistry();

    ControlGroup dcaFader = makeGroup("kDCAFader", "kFader", 1);

    registry.mapContext("dca.0", dcaFader);

    Context context = dca0();

    SystemControlsViewBuilder builder = new SystemControlsViewBuilder();

    List<ViewControl> controls = builder.build(context, registry, null);

    assertTrue(contains(controls, "DCA_FADER"));
  }

  @Test
  public void testCreatesDcaMute() {
    MockCanonicalRegistry registry = new MockCanonicalRegistry();

    ControlGroup mute = makeGroup("kDCAMute", "kDCAMuteOn", 1);

    registry.mapContext("dca.0", mute);

    Context context = dca0();

    List<ViewControl> controls = new SystemControlsViewBuilder().build(context, registry, null);

    assertTrue(contains(controls, "DCA_MUTE"), "Expected DCA mute control");
  }

  @Test
  public void testCreatesDcaFaderAndMute() {
    MockCanonicalRegistry registry = new MockCanonicalRegistry();

    ControlGroup fader = makeGroup("kDCAFader", "kFader", 1);

    ControlGroup mute = makeGroup("kDCAMute", "kDCAMuteOn", 1);

    registry.mapContext("dca.0", fader, mute);

    Context context = dca0();

    List<ViewControl> controls = new SystemControlsViewBuilder().build(context, registry, null);

    assertTrue(contains(controls, "DCA_FADER"));
    assertTrue(contains(controls, "DCA_MUTE"));

    assertEquals(2, controls.size());
  }

  @Test
  public void testNameControlsDoNotBecomeFaders() {

    MockCanonicalRegistry registry = new MockCanonicalRegistry();

    ControlGroup fader = makeGroup("kDCAFader", "kFader", 1);

    ControlGroup mute = makeGroup("kDCAMute", "kDCAMuteOn", 1);

    ControlGroup names = makeGroupMulti("kDCAName", "kNameShort1", "kNameShort2");

    registry.mapContext("dca.0", fader, mute, names);

    List<ViewControl> controls = new SystemControlsViewBuilder().build(dca0(), registry, null);

    long faders = controls.stream().filter(c -> "DCA_FADER".equals(c.logicId)).count();

    assertEquals(1, faders, "DCA name controls must not be rendered as faders");
  }

  @Test
  public void testUnknownDcaControlsIgnored() {

    MockCanonicalRegistry registry = new MockCanonicalRegistry();

    ControlGroup fadeTime =
        makeGroupMulti("kDCAFadeTime", "kFadeLevelOn", "kFadeStartOffset", "kFadeTime");

    registry.mapContext("dca.0", fadeTime);

    List<ViewControl> controls = new SystemControlsViewBuilder().build(dca0(), registry, null);

    assertTrue(controls.isEmpty(), "Unimplemented DCA controls should not render");
  }

  @Test
  public void testDcaRegistryProducesOneFaderAndOneMute() {

    MockCanonicalRegistry registry = new MockCanonicalRegistry();

    ControlGroup fader = makeGroup("kDCAFader", "kFader", 1);

    ControlGroup mute = makeGroup("kDCAMute", "kDCAMuteOn", 1);

    ControlGroup fadeTime =
        makeGroupMulti("kDCAFadeTime", "kFadeLevelOn", "kFadeStartOffset", "kFadeTime");

    ControlGroup names = makeGroupMulti("kDCAName", "kNameShort1", "kNameShort2");

    registry.mapContext("dca.0", fader, mute, fadeTime, names);

    List<ViewControl> controls = new SystemControlsViewBuilder().build(dca0(), registry, null);

    assertEquals(1, controls.stream().filter(c -> "DCA_FADER".equals(c.logicId)).count());

    assertEquals(1, controls.stream().filter(c -> "DCA_MUTE".equals(c.logicId)).count());

    assertEquals(2, controls.size());
  }

  private boolean contains(List<ViewControl> controls, String logicId) {

    return controls.stream().anyMatch(control -> logicId.equals(control.logicId));
  }
}
