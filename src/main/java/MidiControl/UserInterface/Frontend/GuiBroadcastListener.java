package MidiControl.UserInterface.Frontend;

import MidiControl.Controls.ControlInstance;
import MidiControl.UserInterface.CanonicalContextResolver;

public class GuiBroadcastListener {

    private final GuiBroadcaster broadcaster;
    private final CanonicalContextResolver contextResolver;

    public GuiBroadcastListener(GuiBroadcaster broadcaster,
                                CanonicalContextResolver contextResolver) {
        this.broadcaster = broadcaster;
        this.contextResolver = contextResolver;
    }

    public void onControlChanged(ControlInstance instance, int value) {

        String canonicalId = instance.getCanonicalId();
        String contextId = contextResolver.getContextIdForCanonical(canonicalId);

        String json = "{\"type\":\"control-update\",\"payload\":{"
            + "\"canonicalId\":\"" + canonicalId + "\","
            + "\"value\":" + value
            + "}}";

        broadcaster.broadcast(json, contextId);
    }
}