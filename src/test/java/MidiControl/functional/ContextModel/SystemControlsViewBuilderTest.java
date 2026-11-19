package MidiControl.functional.ContextModel;

import static org.junit.jupiter.api.Assertions.*;

import MidiControl.ContextModel.Context;
import MidiControl.ContextModel.ContextDiscoveryEngine;
import MidiControl.ContextModel.ViewBuilders.SystemControlsViewBuilder;
import MidiControl.ContextModel.ViewControl;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.UserInterface.UiContextIndex;
import MidiControl.UserInterface.UiModelFactory;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class SystemControlsViewBuilderTest {
  private CanonicalRegistry registry(String file) {
    List<SysexMapping> mappings =
        SysexMappingLoader.loadMappingsFromResource("MidiControl/" + file + "_sysex_mappings.json");
    return new CanonicalRegistry(mappings, null);
  }

  @Test
  void discovers01v96iMuteMastersAndSoloSettings() {
    CanonicalRegistry registry = registry("01v96i");
    List<Context> contexts = new ContextDiscoveryEngine(registry).discoverContexts();
    Context mute =
        contexts.stream().filter(c -> c.getId().equals("mute.0")).findFirst().orElseThrow();
    Context monitor =
        contexts.stream().filter(c -> c.getId().equals("monitor.0")).findFirst().orElseThrow();
    SystemControlsViewBuilder builder = new SystemControlsViewBuilder();
    List<ViewControl> controls = builder.build(mute, registry, null);
    Set<String> ids = controls.stream().map(c -> c.canonicalId).collect(Collectors.toSet());
    assertEquals(12, ids.size());
    assertTrue(ids.contains("kInMuteGrpMaster.kMasterMute.7"));
    assertTrue(ids.contains("kOutMuteGrpMaster.kMasterMute.3"));
    assertTrue(
        builder.build(monitor, registry, null).stream()
            .anyMatch(c -> c.canonicalId.equals("kSetupSolo.kSoloListen.0")));
    UiContextIndex index = new UiContextIndex();
    index.addAll(contexts);
    assertTrue(
        index.getContextIdsForCanonical("kOutMuteGrpMaster.kMasterMute.3").contains("mute.0"));
  }

  @Test
  void discoversM7clControlsAndPreservesMonitorChoicesInDto() {
    CanonicalRegistry registry = registry("m7cl");
    List<Context> contexts = new ContextDiscoveryEngine(registry).discoverContexts();
    Context mute =
        contexts.stream().filter(c -> c.getId().equals("mute.0")).findFirst().orElseThrow();
    SystemControlsViewBuilder builder = new SystemControlsViewBuilder();
    List<ViewControl> controls = builder.build(mute, registry, null);
    assertEquals(8, controls.size());
    assertEquals("kSceneMuteMaster.kMuteMaster.7", controls.get(7).canonicalId);
    UiContextIndex index = new UiContextIndex();
    index.addAll(contexts);
    var model = new UiModelFactory(registry, builder, index).buildUiModel("monitor.0", null);
    var inputCue =
        model.controls.stream()
            .filter(c -> "kSetupCue.kCueLocInput.0".equals(c.canonicalId))
            .findFirst()
            .orElseThrow();
    assertEquals(
        List.of("PFL", "AFL", "Post Pan"), inputCue.options.stream().map(o -> o.name).toList());
    assertTrue(
        index.getContextIdsForCanonical("kSetupMonitor.kMonitorMono.0").contains("monitor.0"));
  }

  @Test
  void doesNotInventContextsForUnsupportedRegistry() {
    CanonicalRegistry registry = new CanonicalRegistry(List.of(), null);
    List<Context> contexts = new ContextDiscoveryEngine(registry).discoverContexts();
    assertTrue(
        contexts.stream()
            .noneMatch(c -> c.getId().equals("mute.0") || c.getId().equals("monitor.0")));
  }
}
