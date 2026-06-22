package MidiControl.UserInterface;

import MidiControl.ContextModel.*;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.UserInterface.DTO.*;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class UiModelFactory {

    private final CanonicalRegistry registry;
    private final UiContextIndex contextIndex;
    private final DTOMapper mapper = new DTOMapper();
    private final ViewBuilder viewBuilder;
    private static final Logger logger = Logger.getLogger(UiModelFactory.class.getName());

    public UiModelFactory(CanonicalRegistry registry,
                          ViewBuilder viewBuilder,
                          UiContextIndex contextIndex) {

        this.registry = registry;
        this.viewBuilder = Objects.requireNonNull(viewBuilder, "ViewBuilder cannot be null");
        this.contextIndex = contextIndex;
    }

    public UiModelDTO buildUiModel(String contextId, String suffix) {

        Context ctx = contextIndex.getContext(contextId);
        if (ctx == null) return null;

        List<ViewControl> controls = viewBuilder.build(ctx, registry, suffix);

        for (ViewControl vc : controls) {
            if (vc.canonicalId != null && !vc.canonicalId.isBlank()) {
                contextIndex.register(vc.canonicalId, contextId);
            }
            else{
                logger.warning(String.format("View control %s canonical id is blank and is dropped",vc.logicId));
            }
        }

        return mapper.toDto(ctx, controls);
    }
}