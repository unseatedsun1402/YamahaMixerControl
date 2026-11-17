package MidiControl.functional.ContextModel;

import static org.junit.jupiter.api.Assertions.*;

import MidiControl.ContextModel.BusContextDiscoverer;
import MidiControl.ContextModel.ChannelContextDiscoverer;
import MidiControl.ContextModel.Context;
import MidiControl.ContextModel.ControlType;
import MidiControl.ContextModel.ViewBuilders.ChannelDetailViewBuilder;
import MidiControl.ContextModel.ViewControl;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.UserInterface.UiContextIndex;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class ChannelDetailViewBuilderTest {
  private CanonicalRegistry registry(String desk, String file) {
    List<SysexMapping> mappings =
        SysexMappingLoader.loadMappingsFromResource("MidiControl/" + file + "_sysex_mappings.json");
    CanonicalRegistry registry = new CanonicalRegistry(mappings, null);
    registry.setDeskType(desk);
    return registry;
  }

  private List<ViewControl> input(CanonicalRegistry registry, int index) {
    List<Context> contexts = new ArrayList<>();
    new ChannelContextDiscoverer().discover(contexts, registry);
    Context context =
        contexts.stream()
            .filter(c -> c.getId().equals("channel." + index))
            .findFirst()
            .orElseThrow();
    return new ChannelDetailViewBuilder().build(context, registry, null);
  }

  private List<ViewControl> bus(CanonicalRegistry registry, String id) {
    List<Context> contexts = new ArrayList<>();
    new BusContextDiscoverer().discover(contexts, registry);
    Context context = contexts.stream().filter(c -> c.getId().equals(id)).findFirst().orElseThrow();
    return new ChannelDetailViewBuilder().build(context, registry, null);
  }

  private Set<String> ids(List<ViewControl> controls, String role) {
    return controls.stream()
        .filter(c -> role.equals(c.controlRole))
        .map(c -> c.canonicalId)
        .collect(Collectors.toSet());
  }

  @Test
  void discovers01v96iDetailControlsForLastChannel() {
    CanonicalRegistry registry = registry("YAMAHA_01V96I", "01v96i");
    List<ViewControl> controls = input(registry, 39);
    assertEquals(Set.of("kInputAttenuator.kAtt.39"), ids(controls, "INPUT_ATTENUATION"));
    assertEquals(Set.of("kSetupSoloChOn.kSoloChOn.39"), ids(controls, "INPUT_SOLO"));
    assertEquals(Set.of("kChannelInput.kChannelIn.39"), ids(controls, "INPUT_PATCH"));
    ViewControl patch =
        controls.stream()
            .filter(c -> "INPUT_PATCH".equals(c.controlRole))
            .findFirst()
            .orElseThrow();
    assertEquals(ControlType.SELECT, patch.type);
    assertTrue(patch.options.stream().anyMatch(o -> o.source == 1 && !o.name.isBlank()));
    for (int n = 1; n <= 8; n++) {
      assertTrue(ids(controls, "INPUT_SEND_LEVEL").contains("kInputAUX.kAUX" + n + "Level.39"));
      assertTrue(ids(controls, "INPUT_SEND_ON").contains("kInputAUX.kAUX" + n + "On.39"));
      assertTrue(ids(controls, "INPUT_SEND_PRE").contains("kInputAUX.kAUX" + n + "Pre.39"));
      assertTrue(
          ids(controls, "INPUT_BUS_ROUTE").contains("kInputRouting.kRoutingBus" + n + ".39"));
      assertTrue(
          ids(controls, "MUTE_GROUP_ASSIGN").contains("kInputGroup.kInGroupMute" + n + ".39"));
    }
    assertEquals(8, ids(controls, "INPUT_SEND_LEVEL").size());
    assertEquals(8, ids(controls, "INPUT_BUS_ROUTE").size());
    assertEquals(8, ids(controls, "MUTE_GROUP_ASSIGN").size());
    assertEquals(
        Set.of(
            "kAUXOutGroup.kBusGroupMute1.7",
            "kAUXOutGroup.kBusGroupMute2.7",
            "kAUXOutGroup.kBusGroupMute3.7",
            "kAUXOutGroup.kBusGroupMute4.7"),
        ids(bus(registry, "aux.7"), "MUTE_GROUP_ASSIGN"));
  }

  @Test
  void discoversM7clDetailControlsAndMixConfiguration() {
    CanonicalRegistry registry = registry("YAMAHA_M7CL", "m7cl");
    List<ViewControl> controls = input(registry, 55);
    assertEquals(Set.of("kInputAttenuator.kAtt.55"), ids(controls, "INPUT_ATTENUATION"));
    assertEquals(Set.of("kSetupCueInput.kCueOn.55"), ids(controls, "INPUT_SOLO"));
    assertEquals(Set.of("kPatchInInput.kPatchIn.55"), ids(controls, "INPUT_PATCH"));
    for (int n = 1; n <= 16; n++) {
      assertTrue(ids(controls, "INPUT_SEND_LEVEL").contains("kInputToMix.kMix" + n + "Level.55"));
      assertTrue(ids(controls, "INPUT_SEND_ON").contains("kInputToMix.kMix" + n + "On.55"));
      assertTrue(ids(controls, "INPUT_SEND_PRE").contains("kInputToMix.kMix" + n + "Pos.55"));
      assertTrue(ids(controls, "INPUT_SEND_TYPE").contains("kMixBusType.kBusType." + (n - 1)));
    }
    assertEquals(8, ids(controls, "MUTE_GROUP_ASSIGN").size());
    assertFalse(controls.stream().anyMatch(c -> "MUTE".equals(c.logicId)));
    assertTrue(
        ids(bus(registry, "mix.15"), "MUTE_GROUP_ASSIGN").contains("kMixGroup.kMuteAssign8.15"));
    assertTrue(
        ids(bus(registry, "stereo.0"), "MUTE_GROUP_ASSIGN")
            .contains("kStereoGroup.kMuteAssign8.0"));
  }

  @Test
  void mixTypeUpdatesReachAllSubscribedChannelsAndTheMix() {
    CanonicalRegistry registry = registry("YAMAHA_M7CL", "m7cl");
    List<Context> contexts = new ArrayList<>();
    new ChannelContextDiscoverer().discover(contexts, registry);
    new BusContextDiscoverer().discover(contexts, registry);
    UiContextIndex index = new UiContextIndex();
    index.addAll(contexts);
    Set<String> listeners = index.getContextIdsForCanonical("kMixBusType.kBusType.0");
    assertTrue(listeners.containsAll(Set.of("channel.0", "channel.55", "mix.0")));
    index.remove("channel.55");
    assertFalse(index.getContextIdsForCanonical("kMixBusType.kBusType.0").contains("channel.55"));
  }
}
