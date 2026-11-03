import { canonicalIdFor } from "../utils/canonical.js";
import { classifyControl } from "../utils/classify.js";

const PHONE_LANDSCAPE_QUERY =
    "(orientation: landscape) and (max-width: 960px) and (max-height: 520px)";

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

    function updateThumb() {
        setThumbPosition(
            thumb,
            Number(input.value),
            Number(input.min),
            Number(input.max)
        );
    }

    function setValueFromPointer(event) {

        const rect = wrapper.getBoundingClientRect();

        const horizontal = isPhoneLandscape();
        let pct = horizontal
            ? (event.clientX - rect.left) / rect.width
            : (event.clientY - rect.top) / rect.height;

        pct = Math.max(0, Math.min(1, pct));

        if (!horizontal) {
            pct = 1 - pct;
        }

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

        setValueFromPointer(e);

        e.preventDefault();
    });

    wrapper.addEventListener("pointermove", (e) => {

        if (!dragging) {
            return;
        }

        setValueFromPointer(e);

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

function isPhoneLandscape() {
    return window.matchMedia(PHONE_LANDSCAPE_QUERY).matches;
}

function setThumbPosition(thumb, value, min, max) {
    const pct = (value - min) / (max - min);

    thumb.style.setProperty(
        "--fader-position-x",
        `clamp(25px, ${pct * 100}%, calc(100% - 25px))`
    );
    thumb.style.setProperty(
        "--fader-position-y",
        `calc(${(1 - pct) * 100}% - 20px)`
    );
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

        setThumbPosition(thumb, value, min, max);
    }

    
    el.dispatchEvent(
        new CustomEvent("control-update", {
            detail: { value }
        })
    );

}