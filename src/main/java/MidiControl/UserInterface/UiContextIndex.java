package MidiControl.UserInterface;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UiContextIndex implements CanonicalContextResolver {

    private final Map<String, String> canonicalToContext = new ConcurrentHashMap<>();

    public void register(String canonicalId, String contextId) {
        canonicalToContext.put(canonicalId, contextId);
    }

    @Override
    public String getContextIdForCanonical(String canonicalId) {
        return canonicalToContext.get(canonicalId);
    }
}
