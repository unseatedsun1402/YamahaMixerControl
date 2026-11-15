package MidiControl.UserInterface;

import java.util.Set;

public interface CanonicalContextResolver {
  String getContextIdForCanonical(String canonicalId);

  default Set<String> getContextIdsForCanonical(String canonicalId) {
    String context = getContextIdForCanonical(canonicalId);
    return context == null ? Set.of() : Set.of(context);
  }
}
