package MidiControl.UserInterface;

import MidiControl.ContextModel.Context;
import MidiControl.ContextModel.ContextFilter;
import MidiControl.ContextModel.ContextType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class UiContextIndex implements CanonicalContextResolver {

  private static final Logger logger = Logger.getLogger(UiContextIndex.class.getName());

  private final Map<String, String> canonicalToContext = new ConcurrentHashMap<>();
  private final Map<String, Set<String>> canonicalToContexts = new ConcurrentHashMap<>();
  private final Map<String, Context> contextsById = new ConcurrentHashMap<>();
  private final Map<ContextType, List<Context>> contextsByType = new EnumMap<>(ContextType.class);

  private static boolean debug;

  public UiContextIndex() {
    for (ContextType type : ContextType.values()) {
      contextsByType.put(type, new ArrayList<>());
    }
  }

  public static void enableDebug() {
    debug = true;
  }

  /** Add a batch of contexts (called by discovery engine) */
  public void addAll(List<Context> contexts) {
    for (Context ctx : contexts) {
      if (ctx == null || ctx.getId() == null) {
        logger.warning("Attempted to add null context or context with null ID");
        continue;
      }

      contextsById.put(ctx.getId(), ctx);

      contextsByType.get(ctx.getContextType()).add(ctx);

      for (ContextFilter filter : ctx.getFilters()) {
        String group = filter.getControlGroup();
        String sub = filter.getSubControl();
        Integer index = filter.getIndex();

        String canonicalPrefix = group + "." + sub;
        if (index != null) {
          canonicalPrefix += "." + index;
        }

        canonicalToContext.put(canonicalPrefix, ctx.getId());
        canonicalToContexts
            .computeIfAbsent(canonicalPrefix, key -> ConcurrentHashMap.newKeySet())
            .add(ctx.getId());
      }
    }
  }

  public Context getContext(String contextId) {
    return contextsById.get(contextId);
  }

  public List<Context> getByType(ContextType type) {
    return contextsByType.get(type);
  }

  @Override
  public String getContextIdForCanonical(String canonicalId) {

    String exact = canonicalToContext.get(canonicalId);
    if (exact != null) {
      return exact;
    }

    String[] parts = canonicalId.split("\\.");

    if (parts.length == 3) {
      String wildcard = parts[0] + ".*." + parts[2];
      String resolved = canonicalToContext.get(wildcard);

      if (debug && resolved == null && parts[0].contains("kInput")) {
        System.out.println("FAILED " + canonicalId + " wildcard=" + wildcard);
      }

      return resolved;
    }

    System.out.println("FAILED " + canonicalId);
    return null;
  }

  public void register(String canonicalId, String contextId) {
    canonicalToContext.put(canonicalId, contextId);
    canonicalToContexts
        .computeIfAbsent(canonicalId, key -> ConcurrentHashMap.newKeySet())
        .add(contextId);
  }

  @Override
  public Set<String> getContextIdsForCanonical(String canonicalId) {
    Set<String> contexts = new HashSet<>(canonicalToContexts.getOrDefault(canonicalId, Set.of()));
    String[] parts = canonicalId.split("\\.");
    if (parts.length == 3) {
      contexts.addAll(canonicalToContexts.getOrDefault(parts[0] + ".*." + parts[2], Set.of()));
    }
    return contexts;
  }

  public Collection<Context> getAllContexts() {
    return contextsById.values();
  }

  public void remove(String contextId) {
    Context ctx = contextsById.remove(contextId);
    if (ctx != null) {
      contextsByType.get(ctx.getContextType()).remove(ctx);
      canonicalToContexts.values().forEach(contexts -> contexts.remove(contextId));
      canonicalToContext.forEach(
          (key, value) -> {
            if (contextId.equals(value)) {
              Set<String> remaining = canonicalToContexts.getOrDefault(key, Set.of());
              if (remaining.isEmpty()) {
                canonicalToContext.remove(key, value);
              } else {
                canonicalToContext.replace(key, value, remaining.iterator().next());
              }
            }
          });
    }
  }

  public void clear() {
    contextsById.clear();
    canonicalToContext.clear();
    canonicalToContexts.clear();
  }
}
