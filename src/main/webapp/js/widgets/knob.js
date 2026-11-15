import { canonicalIdFor } from "../utils/canonical.js";
import { clamp } from "../utils/math.js";
import { valueToAngle } from "../utils/math.js";
import { classifyControl } from "../utils/classify.js";

export function renderKnob(control) {
    const wrapper = document.createElement("div");
    
    const roleClass = classifyControl(control);
    wrapper.classList.add("rotary-knob");
    if (roleClass) wrapper.classList.add(roleClass);
    if (roleClass === "knob-role-send") {
        const sendMatch = control.canonicalId?.match(/k(?:AUX|Mix)(\d+)Level/i);
        const sendNumber = Number(sendMatch?.[1]);
        if (Number.isInteger(sendNumber) && sendNumber > 0) {
            const sendBank = Math.floor(((sendNumber - 1) % 16) / 4) + 1;
            wrapper.classList.add(`knob-send-bank-${sendBank}`);
        }
    }

    const knob = document.createElement("div");
    knob.className = "knob-dial";

    const valueArc = document.createElement("div");
    valueArc.className = "knob-value";
    knob.appendChild(valueArc);

    wrapper.appendChild(knob);

    let result = wrapper;
    let readout = null;
    if (control.controlRole === "INPUT_ATTENUATION") {
        result = document.createElement("div");
        result.className = "knob-with-readout";
        readout = document.createElement("output");
        readout.className = "knob-readout";
        readout.setAttribute("aria-label", "Attenuation");
        result.append(wrapper, readout);
    }

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

    function updateKnobVisual(val) {
        const angle = valueToAngle(val, uiMin, uiMax);
        valueArc.style.transform = `rotate(${angle}deg)`;
        valueArc.classList.add("updated");
        setTimeout(() => valueArc.classList.remove("updated"), 80);
    }

    function updateValue(raw) {
        control.value = raw;
        uiValue = toUiValue(raw);
        updateKnobVisual(uiValue);
        if (readout) {
            const db = raw / 10;
            readout.textContent = `${db > 0 ? "+" : ""}${db.toFixed(1)} dB`;
        }
    }

    wrapper.updateValue = updateValue;
    updateValue(control.value);

    knob.addEventListener("wheel", (e) => {
        e.preventDefault();
        const delta = e.deltaY < 0 ? 1 : -1;
        uiValue = clamp(uiValue + delta, uiMin, uiMax);
        const rawValue = fromUiValue(uiValue);
        updateValue(rawValue);
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
        updateValue(rawValue);
        const canonicalId = canonicalIdFor(control);
        window.wsClient?.sendControlChange(canonicalId, rawValue);
    }

    function endDrag() {
        document.removeEventListener("pointermove", onDrag);
        document.removeEventListener("pointerup", endDrag);
    }

    knob.addEventListener("dblclick", () => {
        if (control.readOnly) return;
        const rawValue = control.defaultValue;
        if (!Number.isInteger(rawValue) || rawValue < control.min || rawValue > control.max) {
            console.error("Cannot reset knob with invalid default value:", control.canonicalId, rawValue);
            return;
        }
        updateValue(rawValue);
        const canonicalId = canonicalIdFor(control);
        window.wsClient?.sendControlChange(canonicalId, rawValue);
    });

    return result;
}

export function updateKnob(el, rawValue) {
    const knob = el.querySelector(".rotary-knob");
    if (!knob) return;

    knob.updateValue(rawValue);
    
    el.dispatchEvent(
        new CustomEvent("control-update", {
            detail: { rawValue }
        })
    );
}