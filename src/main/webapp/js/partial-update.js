import { updateKnob } from "./widgets/knob.js";
import { updateFader } from "./widgets/fader.js";
import { updateHorizontalSlider } from "./widgets/sliderHorizontal.js"
import { updateButton } from "./widgets/button.js";
import { updateSelect } from "./widgets/select.js";
import { updateToggle } from "./widgets/toggle.js";

export function applyControlUpdate({ canonicalId, value }) {
    const el = document.querySelector(`[data-canonical-id="${canonicalId}"]`);
    if (!el) {
        console.debug(
            "Ignoring update for non-rendered control:",
            canonicalId
        );
        return;
    }

    updateControlValue(el, value);
}

export function updateControlValue(el, value) {
    switch (el.dataset.type) {
        case "FADER":
            updateFader(el, value);
            break;

        case "KNOB":
            updateKnob(el, value);
            break;

        case "SLIDER_HORIZONTAL":
            updateHorizontalSlider(el,value);
            break;
        
        case "TOGGLE":
            updateToggle(el,value);
            break;
        
        case "BUTTON":
            updateButton(el,value);
            break;

        case "SELECT":
            updateSelect(el,value);
            break;

        case "DATA":
            el.dataset.value = value;
            el.dispatchEvent(new CustomEvent("control-update", { detail: { value } }));
            break;

        default:
            console.warn("Unknown control type for partial update:", el.dataset.type);
    }
}
