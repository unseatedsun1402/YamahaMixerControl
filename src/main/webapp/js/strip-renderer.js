// stripRenderer.js

import { renderControl } from "./control-renderer.js";
import { createLedMeter } from "./widgets/meter.js";

export function createStripElement(ctxId) {

    const strip = document.createElement("div");
    strip.className = "channel-strip";
    strip.dataset.contextId = ctxId;
    strip.rebind = model => updateStrip(strip, model);

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
    const shape = JSON.stringify([
        model.viewType,
        model.controls.map(({ value, canonicalId, hwInstance, channelIndex, ...control }) => ({
            ...control,
            sendColor: control.type === "KNOB"
                ? canonicalId?.match(/k(?:AUX|Mix)(\d+)Level/i)?.[1]
                : undefined
        }))
    ]);
    const refreshable = model.controls.every(control =>
        ["FADER", "KNOB", "SLIDER_HORIZONTAL", "TOGGLE", "BUTTON", "SELECT", "DATA"]
            .includes(control.type)
    );

    if (refreshable && stripEl.controlShape === shape) {
        model.controls.forEach((control, index) => stripEl.controlElements[index].rebind(control));
        stripEl.controlElements.forEach(wrapper => wrapper.refresh());
        const preset = stripEl.querySelector(".eq-preset-select");
        if (preset) preset.value = "";
        const presetStatus = stripEl.querySelector(".eq-preset-status");
        if (presetStatus) presetStatus.textContent = "";
        return false;
    }

    scrollArea.innerHTML = "";
    faderArea.innerHTML = "";
    const controlElements = new Map();

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
        if (controls.some(control => control.type === "KNOB")) {
            section.classList.add("section-knob-grid");
        }

        const title = document.createElement("div");
        title.className = "section-title";
        title.textContent = groupName;

        section.appendChild(title);

        for (const control of controls) {
            const wrapper = renderControl(control);
            controlElements.set(control, wrapper);
            section.appendChild(wrapper);
        }

        const containsFader =
            controls.some(c => c.type === "FADER");

        if (containsFader) {
            faderArea.appendChild(section);
        } else {
            scrollArea.appendChild(section);
        }

    }

    stripEl.controlShape = shape;
    stripEl.controlElements = model.controls.map(control => controlElements.get(control));
    return true;
}