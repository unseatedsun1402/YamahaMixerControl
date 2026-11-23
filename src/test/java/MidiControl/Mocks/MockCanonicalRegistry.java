package MidiControl.Mocks;

import MidiControl.ContextModel.ContextFilter;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.ControlInstance;
import MidiControl.Controls.SubControl;
import java.util.*;

public class MockCanonicalRegistry extends CanonicalRegistry {

  private final Map<String, List<ControlInstance>> contextMap = new HashMap<>();
  private final Map<String, ControlInstance> canonicalMap = new HashMap<>();
  private final Map<String, List<ControlInstance>> filterMap = new HashMap<>();

  public MockCanonicalRegistry() {
    super(Collections.emptyList(), null); // SAFE: empty mappings list
  }

  public void mapContext(String contextId, ControlGroup... groups) {

    List<ControlInstance> list = new ArrayList<>();

    for (ControlGroup g : groups) {
      for (SubControl sc : g.getSubcontrols().values()) {
        for (ControlInstance ci : sc.getInstances()) {

          list.add(ci);

          String id = ci.getCanonicalId();
          if (id != null) {
            canonicalMap.put(id, ci);
          }

          String filterKey = g.getName() + "|" + sc.getName() + "|" + ci.getInstanceIndex();

          filterMap.computeIfAbsent(filterKey, ignored -> new ArrayList<>()).add(ci);
        }
      }
    }

    contextMap.put(contextId, list);
  }

  @Override
  public List<ControlInstance> getInstancesForFilter(ContextFilter filter) {

    return filterMap.values().stream()
        .flatMap(List::stream)
        .filter(
            ci ->
                filter.getControlGroup().equals(ci.getGroup())
                    && filter.getIndex() == ci.getInstanceIndex()
                    && ("*".equals(filter.getSubControl())
                        || filter.getSubControl().equals(ci.getSubcontrol())))
        .toList();
  }

  public void registerCanonical(String canonicalId, ControlInstance instance) {
    canonicalMap.put(canonicalId, instance);
  }

  @Override
  public List<ControlInstance> getAllInstancesForContext(String contextId) {
    return contextMap.getOrDefault(contextId, List.of());
  }

  @Override
  public ControlInstance resolveCanonicalId(String canonicalId) {
    return canonicalMap.get(canonicalId);
  }

  @Override
  public Collection<ControlInstance> getAllInstances() {
    System.out.println(String.format("Returning all values of size %d", canonicalMap.size()));
    return canonicalMap.values();
  }
}
