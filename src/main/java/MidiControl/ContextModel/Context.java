package MidiControl.ContextModel;

import java.util.List;

public class Context {
    private final String id;
    private final String label;
    private final ContextType type;
    private final List<String> rolesAllowed;
    private final List<ContextFilter> filters;

    public Context(String id, String label, ContextType type,
                List<String> rolesAllowed, List<ContextFilter> filters) {
        this.id = id;
        this.label = label;
        this.type = type;
        this.rolesAllowed = rolesAllowed;
        this.filters = filters;
    }

    public String getId() { return id; }
    public List<ContextFilter> getFilters() { return filters; }
    public List<String> getRolesAllowed() { return rolesAllowed; }
    public ContextType getContextType() { return type; }
    public String getLabel() { return label; }
}