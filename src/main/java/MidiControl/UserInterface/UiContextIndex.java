package MidiControl.UserInterface;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import MidiControl.ContextModel.Context;
import MidiControl.ContextModel.ContextFilter;

public class UiContextIndex implements CanonicalContextResolver {

    private final Map<String, String> canonicalToContext = new ConcurrentHashMap<>();
    private final Map<String, Context> contextsById = new ConcurrentHashMap<>();
    private static boolean debug;

    public static void enableDebug(){
        debug = true;
    }

    public void addAll(List<Context> contexts) {
        for (Context ctx : contexts) {
            contextsById.put(ctx.getId(), ctx);

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

        String exact = canonicalToContext.get(canonicalId);

        if (exact != null) {
            return exact;
        }

        String[] parts = canonicalId.split("\\.");

        if (parts.length == 3) {

            String wildcard =
                parts[0] + ".*." + parts[2];

            String resolved =
                canonicalToContext.get(wildcard);

            if(debug){if (resolved == null & parts[0].contains("kInput")) {
                System.out.println(
                    "FAILED " +
                    canonicalId +
                    " wildcard=" +
                    wildcard
                );
            }}

            return resolved;
        }

        System.out.println(
            "FAILED " + canonicalId
        );

        return null;
    }

    public void register(String canonicalId, String contextId) {
        canonicalToContext.put(canonicalId, contextId);
    }
    
    public Collection<Context> getAllContexts() {
        return contextsById.values();
    }

}