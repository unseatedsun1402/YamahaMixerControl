package MidiControl.ContextModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.ControlInstance;
import MidiControl.Controls.SubControl;

public class ContextResolver {

    private final Map<String, Context> contexts = new HashMap<>();
    private final Map<String, ControlGroup> controlGroups; // canonical tree

    public ContextResolver(Map<String, ControlGroup> controlGroups) {
        this.controlGroups = controlGroups;
    }

    public void registerContext(Context context) {
        contexts.put(context.getId(), context);
    }

    public Optional<Context> getContext(String id) {
        return Optional.ofNullable(contexts.get(id));
    }

    public List<ControlInstance> resolve(String contextId) {
        Context ctx = contexts.get(contextId);
        if (ctx == null) {
            throw new IllegalArgumentException("Unknown context: " + contextId);
        }

        List<ControlInstance> result = new ArrayList<>();

        for (ContextFilter filter : ctx.getFilters()) {
            resolveFilter(filter, result);
        }

        return result;
    }

    private void resolveFilter(ContextFilter filter, List<ControlInstance> out) {
        ControlGroup group = controlGroups.get(filter.getControlGroup());
        if (group == null) return;

        for (SubControl sub : group.getSubcontrols().values()) {

            // SubControl name match (supports wildcard "*")
            if (!filter.getSubControl().equals("*") &&
                !sub.getName().equals(filter.getSubControl())) {
                continue;
            }

            for (ControlInstance inst : sub.getInstances()) {

                // Index match (null = all)
                if (filter.getIndex() != null &&
                    inst.getIndex() != filter.getIndex()) {
                    continue;
                }

                out.add(inst);
            }
        }
    }
}
