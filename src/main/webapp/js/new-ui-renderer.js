// new-ui-renderer.js
console.log("===== Loaded new-ui-renderer.js (modular, widget-based) =====");

import { renderControl } from "./control-renderer.js";
import { createStripElement } from "./strip-renderer.js";
import { updateStrip } from "./strip-renderer.js";

const renderedStrips = new Map();
const stripContainer = document.getElementById("strip-container");

// ------------------------------------------------------------
// PUBLIC API — called when a full UI model arrives
// ------------------------------------------------------------
export function renderUiModel(model) {
    if (!stripContainer) return;

    const ctxId = model.contextId;

    // Reuse or create strip
    let stripEl = renderedStrips.get(ctxId);
    if (!stripEl) {
        stripEl = createStripElement(ctxId);
        renderedStrips.set(ctxId, stripEl);
        stripContainer.appendChild(stripEl);
    }

    // Update the strip with the new model
    updateStrip(stripEl, model);
}
