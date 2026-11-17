// new-ui-renderer.js
console.log("===== Loaded new-ui-renderer.js (modular, widget-based) =====");

import { createStripElement } from "./strip-renderer.js";
import { createDynamicsWidget } from "./widgets/dynamics.js";
import { createEQWidget } from "./widgets/eq.js"
import { createChannelOnWidget } from "./widgets/channel-on.js"

const renderedStrips = new Map();
const stripContainer = document.getElementById("strip-container");
const stripPool = [];
let pooled = false;

export function setStripContexts(contexts) {
    pooled = true;
    for (const strip of stripPool) {
        strip.querySelectorAll(".rotary-knob, .fader-container, .slider-container")
            .forEach(widget => widget.cancelInteraction?.());
        window.channelMeters?.delete(strip.dataset.contextId);
        strip.remove();
    }
    renderedStrips.clear();

    contexts.forEach((ctxId, index) => {
        const strip = stripPool[index] ?? createStripElement(ctxId);
        stripPool[index] = strip;
        strip.dataset.contextId = ctxId;
        strip.querySelector(".context-title").textContent = ctxId;
        window.channelMeters.set(ctxId, strip.querySelector(".strip-meter"));
        renderedStrips.set(ctxId, strip);
    });
}

export function renderUiModel(model) {

    if (!stripContainer) return;

    const ctxId = model.contextId;

    let stripEl = renderedStrips.get(ctxId);

    if (!stripEl) {
        if (pooled) {
            console.warn("Ignoring model for an unassigned strip:", ctxId);
            return;
        }
        stripEl = createStripElement(ctxId);
        renderedStrips.set(ctxId, stripEl);
    }

    if (!stripEl.parentNode) {
        stripContainer.appendChild(stripEl);
    }

    if (stripEl.rebind(model)) {
        enhanceCompositeWidgets(stripEl);
    }
}

export function getRenderedStrip(ctxId) {
    return renderedStrips.get(ctxId);
}

function enhanceCompositeWidgets(stripEl) {

    stripEl
        .querySelectorAll(".section")
        .forEach(section => {

            const hasDynamics =
                section.querySelector(
                    '[data-role="DYNAMICS1_ON"]'
                );

            const hasEQ =
                section.querySelector(
                    '[data-role^="INPUT_EQ"][data-role$="_GAIN"]'
                );

            const hasChannelOn =
                section.querySelector(
                    '[data-role="INPUT_CHANNEL_ON"]'
                );

            if (hasChannelOn) {
                createChannelOnWidget(section)
            }

            if (hasDynamics) {
                createDynamicsWidget(section);
            }

            if (hasEQ) {
                createEQWidget(section);
            }
        });
}