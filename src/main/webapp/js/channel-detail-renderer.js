// channel-detail-renderer.js
// Renders a single-channel UI model into a console-style detail surface,
// reusing the existing widget factories and composite-widget enhancement.

import { renderControl } from "./control-renderer.js";
import { createLedMeter } from "./widgets/meter.js";
import { createChannelOnWidget } from "./widgets/channel-on.js";
import { createDynamicsWidget } from "./widgets/dynamics.js";
import { createEQWidget } from "./widgets/eq.js";
import { attachChannelDetailVisualizations } from "./widgets/channel-detail-visualizations.js";

/**
 * Maps internal uiGroup names to human-readable section titles.
 */
const SECTION_TITLES = {
    "input.control":        "Channel",
    "input.pan":            "Pan",
    "input.eq":             "EQ",
    "input.dynamics":       "Dynamics",
    "input.send":           "Sends",
    "input.fader":          "Fader",
    "bus.fader":            "Fader",
    "bus.pan":              "Pan",
    "channel.mute":         "Mute",
    "channel.mute-groups":  "Mute Groups",
    "channel.pair":         "Pair",
    "channel.attenuation":  "Attenuation",
    "input.patch":          "Input Patch",
    "input.bus-routing":    "Bus Assignments"
};

/**
 * Desired display order for sections (first = top of the page).
 * Groups not listed here are appended at the end in insertion order.
 */
const SECTION_ORDER = [
    "input.control",
    "channel.mute",
    "channel.mute-groups",
    "channel.pair",
    "input.pan",
    "bus.pan",
    "channel.attenuation",
    "input.patch",
    "input.eq",
    "input.bus-routing",
    "input.dynamics",
    "input.send",
    "input.fader",
    "bus.fader"
];

const COLUMN_GROUPS = {
    left: new Set([
        "input.control",
        "channel.mute",
        "channel.mute-groups",
        "channel.pair",
        "input.pan",
        "bus.pan",
        "channel.attenuation",
        "input.patch"
    ]),
    center: new Set(["input.eq", "input.dynamics", "input.bus-routing"]),
    right: new Set([
        "input.send",
        "input.fader",
        "bus.fader"
    ])
};

let currentPanel = null;

/**
 * Renders a ui-model into #detail-panel as a multi-column detail view.
 *
 * @param {Object} model  The ui-model payload from the WebSocket.
 */
export function renderChannelDetail(model) {
    const container = document.getElementById("detail-panel");
    if (!container) return;

    container.innerHTML = "";

    const panel = document.createElement("div");
    panel.className = "channel-detail";
    panel.dataset.contextId = model.contextId;

    // --- Header: channel name + meter ---

    const header = document.createElement("div");
    header.className = "detail-header";

    const titleArea = document.createElement("div");
    titleArea.className = "detail-title-area";

    const title = document.createElement("span");
    title.className = "detail-title";
    const label = model.metadata?.label || model.contextId;
    title.textContent = label;

    titleArea.appendChild(title);

    const meter = createLedMeter();
    meter.classList.add("detail-meter");
    titleArea.appendChild(meter);

    header.appendChild(titleArea);
    panel.appendChild(header);

    // Register meter so meter-update can find it
    window.channelMeters ??= new Map();
    window.channelMeters.set(model.contextId, meter);

    // --- Controls: group by uiGroup, sort, build sections ---

    const controlsArea = document.createElement("div");
    controlsArea.className = "detail-columns";

    const columns = {};
    for (const columnName of ["left", "center", "right"]) {
        const column = document.createElement("div");
        column.className = `detail-column detail-column-${columnName}`;
        columns[columnName] = column;
        controlsArea.appendChild(column);
    }

    const grouped = {};
    for (const c of model.controls) {
        if (!grouped[c.uiGroup]) grouped[c.uiGroup] = [];
        grouped[c.uiGroup].push(c);
    }

    if (grouped["input.eq"] && grouped["channel.attenuation"]) {
        grouped["input.eq"].push(...grouped["channel.attenuation"]);
        delete grouped["channel.attenuation"];
    }

    const groupNames = Object.keys(grouped).sort((a, b) => {
        const ai = SECTION_ORDER.indexOf(a);
        const bi = SECTION_ORDER.indexOf(b);
        return (ai === -1 ? 999 : ai) - (bi === -1 ? 999 : bi);
    });

    for (const groupName of groupNames) {
        const controls = grouped[groupName];
        const columnName = Object.entries(COLUMN_GROUPS)
            .find(([, groups]) => groups.has(groupName))?.[0] ?? "right";

        const section = document.createElement("div");
        section.className = "section detail-section";
        section.dataset.group = groupName;
        if (groupName !== "input.eq" && controls.some(c => c.type === "KNOB")) {
            section.classList.add("section-knob-grid");
        }

        const sectionTitle = document.createElement("div");
        sectionTitle.className = "section-title";
        sectionTitle.textContent = SECTION_TITLES[groupName] || groupName;
        section.appendChild(sectionTitle);

        if (groupName === "input.send") {
            renderSends(section, controls);
        } else {
            for (const control of controls) {
                section.appendChild(renderControl(control));
            }
        }

        columns[columnName].appendChild(section);
    }

    panel.appendChild(controlsArea);
    container.appendChild(panel);

    // Enhance composite widgets (channel-on, dynamics, EQ)
    enhanceCompositeWidgets(panel);
    attachChannelDetailVisualizations(panel);

    currentPanel = panel;
}

function renderSends(section, controls) {
    const sends = new Map();
    for (const control of controls) {
        if (!sends.has(control.sendIndex)) {
            const send = document.createElement("div");
            send.className = "detail-send";
            sends.set(control.sendIndex, send);
            section.appendChild(send);
        }
        sends.get(control.sendIndex).appendChild(renderControl(control));
    }

    for (const send of sends.values()) {
        const pre = send.querySelector('[data-role="INPUT_SEND_PRE"]');
        if (pre) {
            const button = pre.querySelector("button");
            const updateLabel = () => {
                button.textContent = button.classList.contains("active") ? "Pre" : "Post";
            };
            button.addEventListener("click", updateLabel);
            pre.addEventListener("control-update", updateLabel);
            updateLabel();
        }

        const type = send.querySelector('[data-role="INPUT_SEND_TYPE"]');
        if (type) {
            const updateType = () => {
                const fixed = Number(type.dataset.value) === 2;
                send.classList.toggle("detail-send-fixed", fixed);
                const preButton = pre?.querySelector("button");
                if (preButton) preButton.disabled = fixed || pre.controlMeta.readOnly;
            };
            type.addEventListener("control-update", updateType);
            updateType();
        }
    }
}

export function getDetailPanel() {
    return currentPanel;
}

/**
 * Scans each .detail-section for role markers and upgrades primitives
 * into composite widgets, mirroring the logic in new-ui-renderer.js.
 */
function enhanceCompositeWidgets(panel) {
    panel.querySelectorAll(".section").forEach(section => {
        const hasChannelOn =
            section.querySelector('[data-role="INPUT_CHANNEL_ON"]');
        const hasDynamics =
            section.querySelector('[data-role="DYNAMICS1_ON"]');
        const hasEQ =
            section.querySelector('[data-role^="INPUT_EQ"][data-role$="_GAIN"]');

        if (hasChannelOn) createChannelOnWidget(section);
        if (hasDynamics)  createDynamicsWidget(section);
        if (hasEQ) {
            createEQWidget(section);
            const attenuation = section.querySelector('[data-role="INPUT_ATTENUATION"]');
            const heading = section.querySelector(".eq-widget h4");
            if (attenuation && heading) {
                const header = document.createElement("div");
                header.className = "detail-eq-header";
                heading.replaceWith(header);
                header.append(heading, attenuation);
            }
        }
    });
}
