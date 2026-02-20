import { canonicalIdFor } from "../utils/canonical.js";
import { classifyControl } from "../utils/classify.js";

export function renderFader(control) {
    const wrapper = document.createElement("div");
    wrapper.className = "fader-container";
    
    const roleClass = classifyControl(control);
    if (roleClass) wrapper.classList.add(roleClass);

    // Custom track element
    const track = document.createElement("div");
    track.className = "fader-track";

    // Custom thumb element
    const thumb = document.createElement("div");
    thumb.className = "fader-thumb";

    // Native functional range input (hidden but interactive)
    const input = document.createElement("input");
    input.type = "range";
    input.min = control.min;
    input.max = control.max;
    input.value = control.value;
    input.disabled = control.readOnly;
    input.className = "fader real-input";

    // Thumb height must match thumbnail CSS/SVG
    const thumbHeight = 32; // IMPORTANT: keep synced with CSS

    // Thumb positioning logic (centre‑aligned)
    const updateThumb = () => {
        const min = Number(input.min);
        const max = Number(input.max);
        const val = Number(input.value);

        const pct = 1 - (val - min) / (max - min); // normalised reversed slider % (0 bottom → 1 top)

        thumb.style.top = `calc(${pct * 100}% - ${thumbHeight / 2}px)`;
    };

    updateThumb();

    input.addEventListener("input", () => {
        updateThumb();
        window.wsClient.sendControlChange(
            canonicalIdFor(control),
            Number(input.value)
        );
    });
    
    wrapper.appendChild(track);   // track (middle)
    wrapper.appendChild(thumb);   // thumb (front)
    wrapper.appendChild(input);   // actual functional range input

    return wrapper;
}

export function updateFader(el, value) {
    const input = el.querySelector("input[type=range]");
    if (!input) return;

    const min = Number(input.min);
    const max = Number(input.max);

    if (value < min || value > max) {
        console.error(`Value ${value} out of range: ${min}–${max}`);
        return;
    }

    input.value = value;

    // Move custom thumb
    const thumb = el.querySelector(".fader-thumb");
    if (thumb) {
        const pct = 1 - (value - min) / (max - min);
        const thumbHeight = 32;
        thumb.style.top = `calc(${pct * 100}% - ${thumbHeight / 2}px)`;
    }
}