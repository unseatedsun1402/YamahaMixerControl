package MidiControl.functional.ContextModel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import MidiControl.ContextModel.Context;
import MidiControl.ContextModel.ContextDiscoveryEngine;
import MidiControl.ContextModel.ContextType;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;
import java.util.List;
import org.junit.jupiter.api.Test;

public class DcaContextDiscovererTest {
  @Test
  public void testM7CLDcaDiscovery() {

    List<SysexMapping> mappings =
        SysexMappingLoader.loadMappingsFromResource("MidiControl/m7cl_sysex_mappings.json");

    CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));

    ContextDiscoveryEngine engine = new ContextDiscoveryEngine(registry);

    List<Context> dcaContexts =
        engine.discoverContexts().stream()
            .filter(c -> c.getContextType() == ContextType.DCA)
            .toList();

    assertEquals(8, dcaContexts.size(), "M7CL should expose 8 DCA contexts");
  }

  @Test
  public void testM7CLDcaIdsAreSequential() {

    List<SysexMapping> mappings =
        SysexMappingLoader.loadMappingsFromResource("MidiControl/m7cl_sysex_mappings.json");

    CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));

    ContextDiscoveryEngine engine = new ContextDiscoveryEngine(registry);

    List<String> ids =
        engine.discoverContexts().stream()
            .filter(c -> c.getContextType() == ContextType.DCA)
            .map(Context::getId)
            .toList();

    assertEquals(
        List.of("dca.0", "dca.1", "dca.2", "dca.3", "dca.4", "dca.5", "dca.6", "dca.7"), ids);
  }

  @Test
  public void testM7CLDcaIds() {

    List<SysexMapping> mappings =
        SysexMappingLoader.loadMappingsFromResource("MidiControl/m7cl_sysex_mappings.json");

    CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));

    ContextDiscoveryEngine engine = new ContextDiscoveryEngine(registry);

    List<String> ids =
        engine.discoverContexts().stream()
            .filter(c -> c.getContextType() == ContextType.DCA)
            .map(Context::getId)
            .toList();

    assertEquals(
        List.of("dca.0", "dca.1", "dca.2", "dca.3", "dca.4", "dca.5", "dca.6", "dca.7"), ids);
  }

  @Test
  public void debugM7CLDcaMappings() {

    List<SysexMapping> mappings =
        SysexMappingLoader.loadMappingsFromResource("MidiControl/m7cl_sysex_mappings.json");

    mappings.stream()
        .filter(m -> m.getControlGroup().contains("DCA"))
        .forEach(m -> System.out.println(m.getControlGroup() + " :: " + m.getSubControl()));
  }
}
