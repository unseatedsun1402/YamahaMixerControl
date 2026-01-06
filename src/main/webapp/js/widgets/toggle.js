import { canonicalIdFor } from "../utils/canonical.js";

export function renderToggle(control) {
    const input = document.createElement("input");
    input.type = "checkbox";
    input.checked = control.value > 0;
    input.disabled = control.readOnly;

    input.addEventListener("change", () => {
        sendControlChange(canonicalIdFor(control), input.checked ? 127 : 0);
    });

    return input;
}

export function updateToggle(el, value) {
    const input = el.querySelector("input[type=checkbox]");
    if (!input) return;
    input.checked = value > 0;
}
