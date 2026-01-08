import { canonicalIdFor } from "../utils/canonical.js";

export function renderHorizontalSlider(control) {
    const input = document.createElement("input");
    input.type = "range";
    input.min = control.min;
    input.max = control.max;
    input.value = control.value;
    input.disabled = control.readOnly;
    input.className = "slider-horizontal";

    input.addEventListener("input", () => {
        const canonicalId = canonicalIdFor(control);
        window.wsClient?.sendControlChange(canonicalId, Number(input.value));
    });

    return input;
}

export function updateHorizontalSlider(el, value) {
    const input = el.querySelector("input[type=range]");
    if (!input) return;
    input.value = value;
}
