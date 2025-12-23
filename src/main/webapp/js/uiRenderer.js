console.log("===== Loaded uiRenderer.js (canonical rewrite) =====");

export function renderUiModel(model) {
    const container = document.getElementById("ui-root");
    if (!container) return;

    container.innerHTML = "";

    const strip = document.createElement("div");
    strip.className = "channel-strip";
    container.appendChild(strip);

    const header = document.createElement("div");
    header.className = "context-header";
    header.textContent = `Context: ${model.contextId}`;
    strip.appendChild(header);

    const grouped = {};
    model.controls.forEach(c => {
        if (!grouped[c.uiGroup]) grouped[c.uiGroup] = [];
        grouped[c.uiGroup].push(c);
    });

    const scrollArea = document.createElement("div");
    scrollArea.className = "strip-scroll-area";
    strip.appendChild(scrollArea);

    const faderArea = document.createElement("div");
    faderArea.className = "strip-fader-area";
    strip.appendChild(faderArea);

    for (const [groupName, controls] of Object.entries(grouped)) {
        const section = document.createElement("div");
        section.className = "section";

        const title = document.createElement("div");
        title.className = "section-title";
        title.textContent = groupName;
        section.appendChild(title);

        controls.forEach(control => {
            const el = renderControl(control);
            section.appendChild(el);
        });

        const containsFader = controls.some(c => c.type === "FADER");
        if (containsFader) faderArea.appendChild(section);
        else scrollArea.appendChild(section);
    }
}

function renderControl(control) {
    const wrapper = document.createElement("div");
    wrapper.classList.add(
        "control",
        `control-${control.type.toLowerCase()}`,
        `group-${control.uiGroup}`,
        `index-${control.index}`
    );

    const label = document.createElement("div");
    label.className = "control-label";
    label.textContent = control.label;
    wrapper.appendChild(label);

    let widget;

    switch (control.type) {
        case "FADER":
            widget = renderFader(control);
            break;
        case "KNOB":
            widget = renderKnob(control);
            break;
        case "TOGGLE":
            widget = renderToggle(control);
            break;
        case "BUTTON":
            widget = renderButton(control);
            break;
        case "SELECT":
            widget = renderSelect(control);
            break;
        case "STRING":
            widget = renderStringDisplay(control);
            break;
        case "SLIDER_HORIZONTAL":
            widget = renderHorizontalSlider(control);
            break;
        default:
            widget = document.createElement("div");
            widget.textContent = `[Unknown control type: ${control.type}]`;
    }

    wrapper.appendChild(widget);
    return wrapper;
}

function renderFader(control) {
    const input = document.createElement("input");
    input.type = "range";
    input.min = control.min;
    input.max = control.max;
    input.value = control.value;
    input.disabled = control.readOnly;
    input.className = "fader";

    input.addEventListener("input", () => {
        const canonicalId = `${control.hwGroup}.${control.hwSubcontrol}.${control.hwInstance}`;
        sendControlChange(canonicalId, Number(input.value));
    });

    return input;
}

function renderHorizontalSlider(control) {
    const input = document.createElement("input");
    input.type = "range";
    input.min = control.min;
    input.max = control.max;
    input.value = control.value;
    input.disabled = control.readOnly;
    input.className = "slider-horizontal";

    input.addEventListener("input", () => {
        const canonicalId = `${control.hwGroup}.${control.hwSubcontrol}.${control.hwInstance}`;
        sendControlChange(canonicalId, Number(input.value));
    });

    return input;
}

function renderKnob(control) {
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
        const canonicalId = `${control.hwGroup}.${control.hwSubcontrol}.${control.hwInstance}`;
        sendControlChange(canonicalId, Number(rawValue));
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
        const canonicalId = `${control.hwGroup}.${control.hwSubcontrol}.${control.hwInstance}`;
        sendControlChange(canonicalId, Number(rawValue));
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
        const canonicalId = `${control.hwGroup}.${control.hwSubcontrol}.${control.hwInstance}`;
        sendControlChange(canonicalId, Number(rawValue));
    });

    return wrapper;
}

function clamp(v, min, max) {
    return Math.max(min, Math.min(max, v));
}

function valueToAngle(value, min, max) {
    const range = max - min;
    const percent = (value - min) / range;
    return -135 + percent * 270;
}

function renderToggle(control) {
    const input = document.createElement("input");
    input.type = "checkbox";
    input.checked = control.value > 0;
    input.disabled = control.readOnly;

    input.addEventListener("change", () => {
        const canonicalId = `${control.hwGroup}.${control.hwSubcontrol}.${control.hwInstance}`;
        sendControlChange(canonicalId, input.checked ? 127 : 0);
    });

    return input;
}

function renderButton(control) {
    const btn = document.createElement("button");
    btn.textContent = control.label;
    btn.disabled = control.readOnly;

    btn.addEventListener("click", () => {
        const canonicalId = `${control.hwGroup}.${control.hwSubcontrol}.${control.hwInstance}`;
        sendControlChange(canonicalId, 127);
    });

    return btn;
}

function renderSelect(control) {
    const select = document.createElement("select");
    select.disabled = control.readOnly;

    ["Type A", "Type B", "Type C"].forEach((opt, i) => {
        const o = document.createElement("option");
        o.value = i;
        o.textContent = opt;
        select.appendChild(o);
    });

    select.value = control.value;

    select.addEventListener("change", () => {
        const canonicalId = `${control.hwGroup}.${control.hwSubcontrol}.${control.hwInstance}`;
        sendControlChange(canonicalId, Number(select.value));
    });

    return select;
}

function renderStringDisplay(control) {
    const div = document.createElement("div");
    div.className = "string-display";
    div.textContent = control.value;
    return div;
}

function sendControlChange(canonicalId, value) {
    if (!window.wsClient) return;
    window.wsClient.sendControlChange(canonicalId, Number(value));
}
