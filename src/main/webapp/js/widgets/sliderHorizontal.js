import { canonicalIdFor } from "../utils/canonical.js";
import { doubleTapReset } from "../utils/controls-behaviours.js";

export function renderHorizontalSlider(control) {

    const wrapper = document.createElement("div");
    wrapper.className = "slider-container";

    const track = document.createElement("div");
    track.className = "slider-track";

    const fill = document.createElement("div");
    fill.className = "slider-fill";

    const thumb = document.createElement("div");
    thumb.className = "slider-thumb";

    const input = document.createElement("input");
    input.type = "range";
    input.min = control.min;
    input.max = control.max;
    input.value = control.value;
    input.disabled = control.readOnly;
    input.className = "slider-horizontal real-input";

    track.appendChild(fill);

    function updateSlider() {

        const min = Number(input.min);
        const max = Number(input.max);
        const val = Number(input.value);

        const pct =
            ((val - min) / (max - min));

        thumb.style.left = `${pct * 100}%`;
        fill.style.width = `${pct * 100}%`;
    }

    function setValueFromPointer(clientX) {

        const rect = track.getBoundingClientRect();

        let pct =
            (clientX - rect.left) / rect.width;

        pct = Math.max(0, Math.min(1, pct));

        const min = Number(input.min);
        const max = Number(input.max);

        const value =
            min + (pct * (max - min));

        const rounded = Math.round(value);

        if (rounded === Number(input.value)) {
            return;
        }

        input.value = rounded;

        updateSlider();


        const canonicalId =
            control.canonicalId ??
            canonicalIdFor(control);

        window.wsClient?.sendControlChange(
            canonicalId,
            rounded
        );
    }

    updateSlider();

    let dragging = false;

    wrapper.addEventListener("pointerdown", (e) => {

        if (control.readOnly) {
            return;
        }

        dragging = true;

        wrapper.setPointerCapture(e.pointerId);

        setValueFromPointer(e.clientX);

        e.preventDefault();
    });

    wrapper.addEventListener("pointermove", (e) => {

        if (!dragging) {
            return;
        }

        setValueFromPointer(e.clientX);

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


    doubleTapReset(
        wrapper,
        control.canonicalId ??
            canonicalIdFor(control),
        control.defaultValue
    );

    wrapper.appendChild(track);
    wrapper.appendChild(thumb);
    wrapper.appendChild(input);

    return wrapper;
}

export function updateHorizontalSlider(el, value) {

    const input =
        el.querySelector("input[type=range]");

    if (!input) {
        return;
    }

    const min = Number(input.min);
    const max = Number(input.max);

    input.value = value;

    const pct =
        ((value - min) / (max - min));

    const thumb =
        el.querySelector(".slider-thumb");

    const fill =
        el.querySelector(".slider-fill");

    if (thumb) {
        thumb.style.left = `${pct * 100}%`;
    }

    if (fill) {
        fill.style.width = `${pct * 100}%`;
    }

    el.dispatchEvent(
        new CustomEvent("control-update", {
            detail: { value }
        })
    );
}