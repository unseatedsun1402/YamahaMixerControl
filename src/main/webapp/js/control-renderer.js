// controlRenderer.js
import { renderFader } from "./widgets/fader.js";
import { renderKnob } from "./widgets/knob.js";
import { renderToggle } from "./widgets/toggle.js";
import { renderButton } from "./widgets/button.js";
import { renderSelect } from "./widgets/select.js";
import { renderStringDisplay } from "./widgets/stringDisplay.js";
import { renderHorizontalSlider } from "./widgets/sliderHorizontal.js";

export function renderControl(control) {
    const wrapper = document.createElement("div");
    wrapper.classList.add(
        "control",
        `control-${control.type.toLowerCase()}`,
        `group-${control.uiGroup}`,
        `index-${control.index}`
    );

    // Add canonical ID + type for partial updates
    wrapper.dataset.canonicalId = control.canonicalId;
    wrapper.dataset.type = control.type;
    wrapper.controlMeta = control;

    wrapper.dataset.min = control.min;
    wrapper.dataset.max = control.max;
    wrapper.dataset.uiMin = 0;
    wrapper.dataset.uiMax = 127;
    wrapper.dataset.needsNormalization =
        (control.max - control.min) > 127 ? "1" : "0";

    const label = document.createElement("div");
    label.className = "control-label";
    label.textContent = control.label;
    wrapper.appendChild(label);

    let widget;

    switch (control.type) {
        case "FADER": widget = renderFader(control); break;
        case "KNOB": widget = renderKnob(control); break;
        case "TOGGLE": widget = renderToggle(control); break;
        case "BUTTON": widget = renderButton(control); break;
        case "SELECT": widget = renderSelect(control); break;
        case "STRING": widget = renderStringDisplay(control); break;
        case "SLIDER_HORIZONTAL": widget = renderHorizontalSlider(control); break;
        default:
            widget = document.createElement("div");
            widget.textContent = `[Unknown control type: ${control.type}]`;
    }

    wrapper.appendChild(widget);
    return wrapper;
}
