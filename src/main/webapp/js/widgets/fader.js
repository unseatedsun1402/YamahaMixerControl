import { canonicalIdFor } from "../utils/canonical.js";
import { classifyControl } from "../utils/classify.js";

export function renderFader(control) {

    const wrapper = document.createElement("div");
    wrapper.className = "fader-container";

    const roleClass = classifyControl(control);
    if (roleClass) {
        wrapper.classList.add(roleClass);
    }

    const track = document.createElement("div");
    track.className = "fader-track";

    const thumb = document.createElement("div");
    thumb.className = "fader-thumb";

    // Hidden value holder
    const input = document.createElement("input");
    input.type = "range";
    input.min = control.min;
    input.max = control.max;
    input.value = control.value;
    input.disabled = control.readOnly;
    input.className = "fader real-input";

    const thumbHeight = 40;

    function updateThumb() {

        const min = Number(input.min);
        const max = Number(input.max);
        const val = Number(input.value);

        const pct = 1 - ((val - min) / (max - min));

        thumb.style.top =
            `calc(${pct * 100}% - ${thumbHeight / 2}px)`;
    }

    function setValueFromPointer(clientY) {

        const rect = wrapper.getBoundingClientRect();

        let pct = (clientY - rect.top) / rect.height;

        pct = Math.max(0, Math.min(1, pct));

        // invert so top = max, bottom = min
        pct = 1 - pct;

        const min = Number(input.min);
        const max = Number(input.max);

        const value =
            min + (pct * (max - min));

        const rounded = Math.round(value);

        if (rounded === Number(input.value)) {
            return;
        }

        input.value = rounded;

        updateThumb();

        window.wsClient.sendControlChange(
            canonicalIdFor(control),
            rounded
        );
    }

    updateThumb();

    let dragging = false;

    wrapper.addEventListener("pointerdown", (e) => {

        if (control.readOnly) {
            return;
        }

        dragging = true;

        wrapper.setPointerCapture(e.pointerId);

        setValueFromPointer(e.clientY);

        e.preventDefault();
    });

    wrapper.addEventListener("pointermove", (e) => {

        if (!dragging) {
            return;
        }

        setValueFromPointer(e.clientY);

        e.preventDefault();
    });

    wrapper.addEventListener("pointerup", (e) => {

        dragging = false;

        try {
            wrapper.releasePointerCapture(e.pointerId);
        } catch (_) {}

        e.preventDefault();
    });

    wrapper.addEventListener("pointercancel", () => {
        dragging = false;
    });

    wrapper.appendChild(track);
    wrapper.appendChild(thumb);
    wrapper.appendChild(input);

    return wrapper;
}

export function updateFader(el, value) {

    const input = el.querySelector("input[type=range]");
    if (!input) {
        return;
    }

    const min = Number(input.min);
    const max = Number(input.max);

    if (value < min || value > max) {
        console.error(
            `Value ${value} out of range: ${min}–${max}`
        );
        return;
    }

    input.value = value;

    const thumb = el.querySelector(".fader-thumb");

    if (thumb) {

        const thumbHeight = 40;

        const pct =
            1 - ((value - min) / (max - min));

        thumb.style.top =
            `calc(${pct * 100}% - ${thumbHeight / 2}px)`;
    }
}