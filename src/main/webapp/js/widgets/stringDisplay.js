import { canonicalIdFor } from "../utils/canonical.js";

export function renderStringDisplay(control) {
    const div = document.createElement("div");
    div.className = "string-display";
    div.textContent = control.value;
    return div;
}

export function updateStringDisplay(el, value){
    el.textContent = value
}