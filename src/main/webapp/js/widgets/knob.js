import { canonicalIdFor } from "../utils/canonical.js";
import { clamp } from "../utils/math.js";
import { valueToAngle } from "../utils/math.js";

export function renderKnob(control) {
    const wrapper = document.createElement("div");
    wrapper.className = "rotary-knob";

    const knob = document.createElement("div");
    knob.className = "knob-dial";

    const valueArc = document.createElement("div");
    valueArc.className = "knob-value";
    knob.appendChild(valueArc);

    wrapper.appendChild(knob);

    const needsNormalization = (control.max - control.min) > 127;
    const uiMin = 0;
    const uiMax = 127;

    function toUiValue(raw) {
        if (!needsNormalization) return raw;
        return Math.round((raw - control.min) / (control.max - control.min) * uiMax);
    }

    function fromUiValue(ui) {
        if (!needsNormalization) return ui;
        return Math.round(ui / uiMax * (control.max - control.min) + control.min);
    }

    let uiValue = toUiValue(control.value);

    updateKnobVisual(uiValue);

    function updateKnobVisual(val) {
        const angle = valueToAngle(val, uiMin, uiMax);
        valueArc.style.transform = `rotate(${angle}deg)`;
        valueArc.classList.add("updated");
        setTimeout(() => valueArc.classList.remove("updated"), 80);
    }

    knob.addEventListener("wheel", (e) => {
        e.preventDefault();
        const delta = e.deltaY < 0 ? 1 : -1;
        uiValue = clamp(uiValue + delta, uiMin, uiMax);
        const rawValue = fromUiValue(uiValue);
        control.value = rawValue;
        updateKnobVisual(uiValue);
        const canonicalId = canonicalIdFor(control);
        window.wsClient?.sendControlChange(canonicalId, rawValue);
    });

    let startY = null;
    let startUiValue = null;

    knob.addEventListener("pointerdown", (e) => {
        e.preventDefault();
        startY = e.clientY;
        startUiValue = uiValue;

        document.addEventListener("pointermove", onDrag);
        document.addEventListener("pointerup", endDrag);
    });

    function onDrag(e) {
        const deltaY = startY - e.clientY;
        const sensitivity = 0.5;
        uiValue = clamp(startUiValue + deltaY * sensitivity, uiMin, uiMax);
        uiValue = Math.round(uiValue);
        const rawValue = fromUiValue(uiValue);
        control.value = rawValue;
        updateKnobVisual(uiValue);
        const canonicalId = canonicalIdFor(control);
        window.wsClient?.sendControlChange(canonicalId, rawValue);
    }

    function endDrag() {
        document.removeEventListener("pointermove", onDrag);
        document.removeEventListener("pointerup", endDrag);
    }

    knob.addEventListener("dblclick", () => {
        uiValue = Math.round((uiMin + uiMax) / 2);
        const rawValue = fromUiValue(uiValue);
        control.value = rawValue;
        updateKnobVisual(uiValue);
        const canonicalId = canonicalIdFor(control);
        window.wsClient?.sendControlChange(canonicalId, rawValue);
    });

    return wrapper;
}

export function updateKnob(el, rawValue) {
    console.log("updateKnob called on:", el, "canonical:", el.dataset.canonicalId);
    const knob = el.querySelector(".rotary-knob");
    if (!knob) return;

    const valueArc = knob.querySelector(".knob-value");
    if (!valueArc) return;

    const min = Number(el.dataset.min);
    const max = Number(el.dataset.max);
    const uiMin = Number(el.dataset.uiMin);
    const uiMax = Number(el.dataset.uiMax);
    const needsNormalization = el.dataset.needsNormalization === "1";

    const uiValue = needsNormalization
        ? Math.round((rawValue - min) / (max - min) * uiMax)
        : rawValue;

    const angle = valueToAngle(uiValue, uiMin, uiMax);
    valueArc.style.transform = `rotate(${angle}deg)`;
}