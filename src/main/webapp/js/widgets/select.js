import { canonicalIdFor } from "../utils/canonical.js";

export function renderSelect(control) {
    const select = document.createElement("select");
    select.disabled = control.readOnly;

    ["Type A", "Type B"].forEach((opt, i) => {
        const o = document.createElement("option");
        o.value = i;
        o.textContent = opt;
        select.appendChild(o);
    });

    select.value = control.value;

    select.addEventListener("change", () => {
        sendControlChange(canonicalIdFor(control), Number(select.value));
    });

    return select;
}

export function updateSelect(el, value) {
    const select = el.querySelector("select");
    if (!select) return;
    select.value = value;
}
