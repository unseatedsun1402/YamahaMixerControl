package MidiControl.UserInterface.Frontend;

import java.util.logging.Logger;

import MidiControl.Controls.ControlInstance;
import MidiControl.UserInterface.CanonicalContextResolver;
import MidiControl.UserInterface.DTO.ControlUpdateDTO;

public class GuiBroadcastListener {

    private final GuiBroadcaster broadcaster;
    private final CanonicalContextResolver contextResolver;
    private static final Logger logger = Logger.getLogger(GuiBroadcastListener.class.getName());

    public GuiBroadcastListener(GuiBroadcaster broadcaster,
                                CanonicalContextResolver contextResolver) {
        this.broadcaster = broadcaster;
        this.contextResolver = contextResolver;
    }

    public void onControlChanged(ControlInstance instance, int value) {
        try {
            if (instance == null) {
                logger.warning("GuiBroadcastListener: instance was null");
                return;
            }

            String canonicalId = instance.getCanonicalId();
            if (canonicalId == null) {
                logger.warning("GuiBroadcastListener: canonicalId was null");
                return;
            }

            // Context may legitimately be null during hydration
            String contextId = contextResolver.getContextIdForCanonical(canonicalId);
            if (contextId == null) {
                // UI not interested in this control yet — safe to ignore
                return;
            }

            ControlUpdateDTO dto = new ControlUpdateDTO();
            dto.canonicalId = canonicalId;
            dto.value = value;

            String json = dto.toJson();
            if (json == null) {
                logger.warning("GuiBroadcastListener: dto.toJson() returned null");
                return;
            }

            // Broadcaster may be null or may throw if context is not registered
            try {
                broadcaster.broadcast(json, contextId);
            } catch (Exception ex) {
                logger.warning("GuiBroadcastListener: broadcast failed for " + canonicalId 
                            + " context=" + contextId + " error=" + ex);
            }

        } catch (Exception e) {
            logger.severe("GuiBroadcastListener.onControlChanged unexpected exception: " + e);
        }
    }
}