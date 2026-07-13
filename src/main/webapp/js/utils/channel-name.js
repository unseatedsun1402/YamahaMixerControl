import { getRenderedStrip } from "../new-ui-renderer.js";

export function updateChannelStripName(contextId, name) {

    const strip = getRenderedStrip(contextId);

    if (!strip) {
        return;
    }

    const label = strip.querySelector(".context-header");

    if (!label) {
        console.warn(
            "Strip exists but has no .context-header:",
            contextId
        );
        return;
    }

    label.textContent = name;
}