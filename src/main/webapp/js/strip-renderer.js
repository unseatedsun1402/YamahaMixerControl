// stripRenderer.js

import { renderControl } from "./control-renderer.js";
import { createLedMeter } from "./widgets/meter.js";

export function createStripElement(ctxId) {

    const strip = document.createElement("div");
    strip.className = "channel-strip";
    strip.dataset.contextId = ctxId;

    // Header
    const header = document.createElement("div");
    header.className = "context-header";

    const title = document.createElement("span");
    title.className = "context-title";
    title.textContent = ctxId;

    const meter = createLedMeter();
    meter.classList.add("strip-meter");

    header.appendChild(title);
    header.appendChild(meter);

    strip.appendChild(header);

    // Register meter
    window.channelMeters ??= new Map();
    window.channelMeters.set(ctxId, meter);

    // Scroll area
    const scrollArea = document.createElement("div");
    scrollArea.className = "strip-scroll-area";
    strip.appendChild(scrollArea);

    // Fader area
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

    const grouped = {};

    for (const c of model.controls) {
        if (!grouped[c.uiGroup]) {
            grouped[c.uiGroup] = [];
        }
        grouped[c.uiGroup].push(c);
    }

    for (const [groupName, controls] of Object.entries(grouped)) {

        const section = document.createElement("div");
        section.className = "section";

        const title = document.createElement("div");
        title.className = "section-title";
        title.textContent = groupName;

        section.appendChild(title);

        for (const control of controls) {
            section.appendChild(renderControl(control));
        }

        const containsFader =
            controls.some(c => c.type === "FADER");

        if (containsFader) {
            faderArea.appendChild(section);
        } else {
            scrollArea.appendChild(section);
        }
    }
}