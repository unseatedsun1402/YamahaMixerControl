import { canonicalIdFor } from "../utils/canonical.js";

export function renderFader(control) {
    const input = document.createElement("input");
    input.type = "range";
    input.min = control.min;
    input.max = control.max;
    input.value = control.value;
    input.disabled = control.readOnly;
    input.className = "fader";

    input.addEventListener("input", () => {
        window.wsClient.sendControlChange(
            canonicalIdFor(control),Number(input.value));
    });

    return input;
}

export function updateFader(el, value) {
    const input = el.querySelector("input[type=range]");
    if (!input) return;
    input.value = value;
}