// new-ui-renderer.js
console.log("===== Loaded new-ui-renderer.js (modular, widget-based) =====");

import { renderControl } from "./control-renderer.js";
import { createStripElement } from "./strip-renderer.js";
import { updateStrip } from "./strip-renderer.js";
import { createDynamicsWidget } from "./widgets/dynamics.js";
import { createEQWidget } from "./widgets/eq.js"

const renderedStrips = new Map();
const stripContainer = document.getElementById("strip-container");

export function renderUiModel(model) {

    if (!stripContainer) return;

    const ctxId = model.contextId;

    let stripEl = renderedStrips.get(ctxId);

    if (!stripEl) {
        stripEl = createStripElement(ctxId);
        renderedStrips.set(ctxId, stripEl);
    }

    if (!stripEl.parentNode) {
        stripContainer.appendChild(stripEl);
    }

    updateStrip(stripEl, model);

    enhanceCompositeWidgets(stripEl);
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
                    '[data-role="INPUT_EQ1_GAIN"]'
                );

            if (hasDynamics) {
                createDynamicsWidget(section);
            }

            if (hasEQ) {
                createEQWidget(section);
            }
        });
}