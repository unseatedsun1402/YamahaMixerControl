import { canonicalIdFor } from "../utils/canonical.js";

export function renderSelect(control) {
    const wrapper = document.createElement("div");
    wrapper.dataset.canonicalId = control.canonicalId;
    wrapper.dataset.type = "SELECT";

    const select = document.createElement("select");
    select.disabled = control.readOnly;

    // Build source list dynamically
    for (let i = control.min; i <= control.max; i++) {
        const o = document.createElement("option");
        o.value = i;
        o.textContent = `Source ${i}`;
        select.appendChild(o);
    }

    select.value = control.value;

    select.addEventListener("change", () => {
        const canonicalId = canonicalIdFor(control);
        console.log("SELECT canonicalId:", canonicalId);
        window.wsClient?.sendControlChange(canonicalId, Number(select.value));
    });

    wrapper.appendChild(select);
    return wrapper;
}

export function updateSelect(el, value) {
    const select = el.querySelector("select");
    if (!select) return;
    select.value = value;

    el.dispatchEvent(
        new CustomEvent("control-update", {
            detail: { value }
        })
    );

}
