package MidiControl.ContextModel;

import java.util.Map;

public class BankFilter {

    // e.g. "channel", "mix", "matrix", "dca", "stereo"
    private final String prefix;

    // optional index (null = match all)
    private final Integer index;

    // optional context type (CHANNEL, MIX, MATRIX, etc.)
    private final ContextType type;

    // optional metadata matchers (loose typed)
    private final Map<String, Object> metadata;

    public BankFilter(String prefix, Integer index, ContextType type, Map<String, Object> metadata) {
        this.prefix = prefix;
        this.index = index;
        this.type = type;
        this.metadata = metadata;
    }

    public BankFilter(String prefix, Integer index, ContextType type) {
        this(prefix, index, type, null);
    }

    public BankFilter(String prefix, ContextType type) {
        this(prefix, null, type, null);
    }

    public BankFilter(String prefix) {
        this(prefix, null, null, null);
    }

    public String getPrefix() { return prefix; }
    public Integer getIndex() { return index; }
    public ContextType getType() { return type; }
    public Map<String, Object> getMetadata() { return metadata; }
}
