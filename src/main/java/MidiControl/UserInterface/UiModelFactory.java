package MidiControl.UserInterface;

import MidiControl.ContextModel.*;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.UserInterface.DTO.*;

import java.util.List;

public class UiModelFactory {

    private final CanonicalRegistry registry;
    private final ControlSchema schema;
    private final ViewBuilder viewBuilder;
    private final UiContextIndex contextIndex;

    private final DTOMapper mapper = new DTOMapper();

    public UiModelFactory(CanonicalRegistry registry,
                          ControlSchema schema,
                          ViewBuilder viewBuilder,
                          UiContextIndex contextIndex) {
        this.registry = registry;
        this.schema = schema;
        this.viewBuilder = viewBuilder;
        this.contextIndex = contextIndex;
    }

    public UiModelDTO buildUiModel(String contextId) {

        Context ctx = ContextFactory.fromId(contextId, registry);
        if (ctx == null) return null;

        List<ViewControl> controls = viewBuilder.build(ctx, registry);

        // Register canonicalId → contextId
        for (ViewControl vc : controls) {
            contextIndex.register(vc.getCanonicalId(), contextId);
        }

        // Convert to DTO (no layout, no backend objects)
        return mapper.toDto(ctx, controls);
    }
}