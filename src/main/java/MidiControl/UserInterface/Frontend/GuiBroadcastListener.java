package MidiControl.UserInterface.Frontend;

import java.util.logging.Logger;

import MidiControl.Controls.ControlInstance;
import MidiControl.UserInterface.CanonicalContextResolver;
import MidiControl.UserInterface.DTO.ControlUpdateDTO;

public class GuiBroadcastListener {

    private final GuiBroadcaster broadcaster;
    private volatile CanonicalContextResolver contextResolver;
    private static final Logger logger = Logger.getLogger(GuiBroadcastListener.class.getName());
    private static boolean debug = false;

    public static void enableDebug(){
        debug = true;
    }

    public GuiBroadcastListener(GuiBroadcaster broadcaster,
                                CanonicalContextResolver contextResolver) {
        this.broadcaster = broadcaster;
        this.contextResolver = contextResolver;
    }

    public void onControlChanged(ControlInstance instance, int value) {
        if(debug)logger.info(
            String.format(
                "GUI UPDATE %s = %d",
                instance != null ? instance.getCanonicalId() : "NULL",
                value
            )
        );
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
            if(debug)logger.info(
                String.format(
                    "RESOLVE %s -> %s",
                    canonicalId,
                    contextId
                )
            );
            if (contextId == null) {
                // UI not interested in this control yet — safe to ignore
                return;
            }

            ControlUpdateDTO dto = new ControlUpdateDTO();
            dto.canonicalId = canonicalId;
            dto.value = value;
            dto.min = instance.getMin();
            dto.max = instance.getMax();

            String json = dto.toJson();
            if (json == null) {
                logger.warning("GuiBroadcastListener: dto.toJson() returned null");
                return;
            }

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

    public void reloadContextIndex(CanonicalContextResolver resolver){
        this.contextResolver = resolver;
    }
}