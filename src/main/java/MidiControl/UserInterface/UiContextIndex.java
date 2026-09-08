package MidiControl.UserInterface;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import MidiControl.ContextModel.Context;
import MidiControl.ContextModel.ContextFilter;
import MidiControl.ContextModel.ContextType;

public class UiContextIndex implements CanonicalContextResolver {

    private static final Logger logger =
        Logger.getLogger(UiContextIndex.class.getName());

    private final Map<String, String> canonicalToContext = new ConcurrentHashMap<>();
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

    /**
     * Add a batch of contexts (called by discovery engine)
     */
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
    }

    public Collection<Context> getAllContexts() {
        return contextsById.values();
    }

    public void remove(String contextId) {
        Context ctx = contextsById.remove(contextId);
        if (ctx != null) {
            contextsByType.get(ctx.getContextType()).remove(ctx);
        }
    }

    public void clear() {
        contextsById.clear();
        canonicalToContext.clear();
    }
}
