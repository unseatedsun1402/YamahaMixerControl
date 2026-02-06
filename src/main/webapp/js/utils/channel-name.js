export function updateChannelStripName(contextId, name) {
    const strip = document.querySelector(`[data-context-id="${contextId}"]`);
    if (!strip) {
        console.warn("Received name-update but strip not found:", contextId);
        return;
    }

    const label = strip.querySelector(".context-header");
    if (!label) {
        console.warn("Strip exists but has no label .strip-label:", contextId);
        return;
    }

    label.textContent = name;
}