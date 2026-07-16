import { canonicalIdFor } from "../utils/canonical.js";

// export function renderToggle(control) {
//     const input = document.createElement("input");
//     input.type = "checkbox";
//     input.checked = control.value > 0;
//     input.disabled = control.readOnly;
//     input.dataset.canonicalId=control.canonicalId

//     input.addEventListener("change", () => {
//         const canonicalId = canonicalIdFor(control);
//         window.wsClient?.sendControlChange(
//             canonicalId,
//             input.checked ? 127 : 0
//         );
//     });

//     return input;
// }

export function renderToggle(control) {

    const button = document.createElement("button");

    button.className = "toggle-button";
    button.textContent = control.label;

    if (control.value > 0) {
        button.classList.add("active");
    }

    button.addEventListener("click", () => {

        const nextValue =
            button.classList.contains("active")
                ? 0
                : 127;

        button.classList.toggle("active");

        window.wsClient?.sendControlChange(
            canonicalIdFor(control),
            nextValue
        );
    });

    return button;
}

// export function updateToggle(el, value) {
//     const input = el.querySelector("input[type=checkbox]");
//     if (!input) return;
//     input.checked = value > 0;

//     el.dispatchEvent(
//         new CustomEvent("control-update", {
//             detail: { value }
//         })
//     );
// }

export function updateToggle(el, value) {

    const button =
        el.querySelector(".toggle-button");

    if (!button) {
        return;
    }

    button.classList.toggle(
        "active",
        value > 0
    );

    el.dispatchEvent(
        new CustomEvent(
            "control-update",
            {
                detail: { value }
            }
        )
    );
}
