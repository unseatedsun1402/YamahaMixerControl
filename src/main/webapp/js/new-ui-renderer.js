// new-ui-renderer.js
console.log("===== Loaded new-ui-renderer.js (modular, widget-based) =====");

import { renderControl } from "./control-renderer.js";
import { createStripElement } from "./strip-renderer.js";
import { updateStrip } from "./strip-renderer.js";

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
}

export function getRenderedStrip(ctxId) {
    return renderedStrips.get(ctxId);
}
