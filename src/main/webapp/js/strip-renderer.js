// stripRenderer.js
import { renderControl } from "./control-renderer.js";

export function createStripElement(ctxId) {
    const strip = document.createElement("div");
    strip.className = "channel-strip";
    strip.dataset.contextId = ctxId;

    // Header
    const header = document.createElement("div");
    header.className = "context-header";
    header.textContent = ctxId;
    strip.appendChild(header);

    // Scrollable area for knobs, buttons, toggles, etc.
    const scrollArea = document.createElement("div");
    scrollArea.className = "strip-scroll-area";
    strip.appendChild(scrollArea);

    // Fader area (fixed bottom)
    const faderArea = document.createElement("div");
    faderArea.className = "strip-fader-area";
    strip.appendChild(faderArea);

    return strip;
}

export function updateStrip(stripEl, model) {
    const scrollArea = stripEl.querySelector(".strip-scroll-area");
    const faderArea = stripEl.querySelector(".strip-fader-area");

    scrollArea.innerHTML = "";
    faderArea.innerHTML = "";

    // Group controls by uiGroup
    const grouped = {};
    for (const c of model.controls) {
        if (!grouped[c.uiGroup]) grouped[c.uiGroup] = [];
        grouped[c.uiGroup].push(c);
    }

    // Render each group
    for (const [groupName, controls] of Object.entries(grouped)) {
        const section = document.createElement("div");
        section.className = "section";

        const title = document.createElement("div");
        title.className = "section-title";
        title.textContent = groupName;
        section.appendChild(title);

        for (const control of controls) {
            const el = renderControl(control);
            section.appendChild(el);
        }

        const containsFader = controls.some(c => c.type === "FADER");
        if (containsFader) faderArea.appendChild(section);
        else scrollArea.appendChild(section);
    }
}
