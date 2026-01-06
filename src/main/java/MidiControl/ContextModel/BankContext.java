package MidiControl.ContextModel;

import java.util.ArrayList;
import java.util.List;

public class BankContext {
    private final List<BankFilter> filters = new ArrayList<>();

    public void addFilter(BankFilter filter) {
        filters.add(filter);
    }

    public List<BankFilter> getFilters() {
        return filters;
    }
}
