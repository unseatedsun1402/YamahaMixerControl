import { canonicalIdFor } from "../utils/canonical.js";

export function renderButton(control) {
    const btn = document.createElement("button");
    btn.textContent = control.label;
    btn.disabled = control.readOnly;

    btn.addEventListener("click", () => {
        sendControlChange(canonicalIdFor(control), 127);
    });

    return btn;
}

export function updateButton(el, value) {
    const btn = el.querySelector("button");
    if (!btn) return;
    btn.classList.add("flash");
    setTimeout(() => btn.classList.remove("flash"), 100);
}
