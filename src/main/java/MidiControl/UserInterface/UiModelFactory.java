package MidiControl.UserInterface;

import MidiControl.ContextModel.*;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.UserInterface.DTO.*;

import java.util.List;
import java.util.Objects;

public class UiModelFactory {

    private final CanonicalRegistry registry;
    private final UiContextIndex contextIndex;
    private final DTOMapper mapper = new DTOMapper();
    private final ViewBuilder viewBuilder;

    public UiModelFactory(CanonicalRegistry registry,
                          ViewBuilder viewBuilder,
                          UiContextIndex contextIndex) {

        this.registry = registry;
        this.viewBuilder = Objects.requireNonNull(viewBuilder, "ViewBuilder cannot be null");
        this.contextIndex = contextIndex;
    }

    public UiModelDTO buildUiModel(String contextId) {

        Context ctx = contextIndex.getContext(contextId);

        if (ctx == null) return null;

        List<ViewControl> controls = viewBuilder.build(ctx, registry);

        for (ViewControl vc : controls) {
            contextIndex.register(vc.getCanonicalId(), contextId);
        }

        return mapper.toDto(ctx, controls);
    }
}