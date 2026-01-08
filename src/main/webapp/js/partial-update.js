import { updateKnob } from "./widgets/knob.js";
import { updateFader } from "./widgets/fader.js";
import { updateHorizontalSlider } from "./widgets/sliderHorizontal.js"
import { updateButton } from "./widgets/button.js";
import { updateSelect } from "./widgets/select.js";
import { updateToggle } from "./widgets/toggle.js";

export function applyControlUpdate({ canonicalId, value }) {
    const el = document.querySelector(`[data-canonical-id="${canonicalId}"]`);
    if (!el) {
        console.warn("Partial update: control not found:", canonicalId);
        return;
    }

    const type = el.dataset.type; // e.g. "FADER", "KNOB"

    switch (type) {
        case "FADER":
            updateFader(el, value);
            break;

        case "KNOB":
            updateKnob(el, value);
            break;

        case "HORIZONTAL_SLIZDER":
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

        default:
            console.warn("Unknown control type for partial update:", type);
    }
}