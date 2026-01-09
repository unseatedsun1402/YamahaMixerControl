package MidiControl.UserInterface;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import MidiControl.ContextModel.Context;
import MidiControl.ContextModel.ContextFilter;

public class UiContextIndex implements CanonicalContextResolver {

    private final Map<String, String> canonicalToContext = new ConcurrentHashMap<>();
    private final Map<String, Context> contextsById = new ConcurrentHashMap<>();

    public void addAll(List<Context> contexts) {
        for (Context ctx : contexts) {
            // Store the actual Context object
            contextsById.put(ctx.getId(), ctx);

            // Store canonical → context mappings
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

    @Override
    public String getContextIdForCanonical(String canonicalId) {
        return canonicalToContext.get(canonicalId);
    }

    public void register(String canonicalId, String contextId) {
        canonicalToContext.put(canonicalId, contextId);
    }
}