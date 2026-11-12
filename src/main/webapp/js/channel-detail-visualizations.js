const SVG_NS = "http://www.w3.org/2000/svg";
const EQ_BAND_COUNT = 4;
const EQ_BAND_LABELS = ["LOW", "LMID", "HMID", "HIGH"];
const EQ_MIN_FREQUENCY = 20;
const EQ_MAX_FREQUENCY = 20000;
const GRAPH_WIDTH = 600;
const GRAPH_HEIGHT = 190;
const PLOT = { left: 44, right: 12, top: 12, bottom: 30 };
const COMPRESSOR_RATIOS = [
    1, 1.1, 1.3, 1.5, 1.7, 2, 2.5, 3,
    4, 5, 6, 8, 10, 20, Infinity, Infinity
];

export function attachChannelDetailVisualizations(panel) {
    const eqSection = panel.querySelector(
        '[data-role^="INPUT_EQ"][data-role$="_GAIN"]'
    )?.closest(".detail-section");
    if (eqSection) addEqResponseGraph(eqSection);

    const dynamicsSection = panel.querySelector(
        '[data-role="DYNAMICS2_ON"]'
    )?.closest(".detail-section");
    if (dynamicsSection) addDynamicsGraphs(dynamicsSection);
}

function addEqResponseGraph(section) {
    const widget = section.querySelector(".eq-widget");
    if (!widget) return;

    const graph = createGraph("eq-response-graph", "EQ frequency response");
    widget.insertBefore(graph, widget.querySelector(".eq-preset-label"));

    const update = () => {
        const bands = Array.from({ length: EQ_BAND_COUNT }, (_, index) => {
            const band = index + 1;
            const gain = findControl(section, `INPUT_EQ${band}_GAIN`);
            const frequency = findControl(section, `INPUT_EQ${band}_FREQUENCY`);
            const q = findControl(section, `INPUT_EQ${band}_Q`);
            if (!gain || !frequency || !q) return null;
            const gainRow = Array.from(widget.querySelectorAll(".eq-row"))
                .find(row => row.querySelector("label")?.textContent === EQ_BAND_LABELS[index]);
            const visibleGain = Number(
                gainRow?.querySelector('.slider-container input[type="range"]')?.value
            );

            return {
                gainDb: gainToDb(
                    gain,
                    Number.isFinite(visibleGain) ? visibleGain : controlValue(gain)
                ),
                frequencyHz: frequencyToHz(frequency),
                q: qToValue(q)
            };
        }).filter(Boolean);

        drawEqResponse(graph, bands);
    };

    section.querySelectorAll('[data-role^="INPUT_EQ"]').forEach(control => {
        control.addEventListener("control-update", update);
    });
    widget.addEventListener("input", update);
    widget.addEventListener("pointermove", update);
    update();
}

function addDynamicsGraphs(section) {
    const widget = section.querySelector(".dynamics-widget");
    if (!widget) return;

    const gateGraph = createGraph("gate-response-graph", "Gate response");
    const compressorGraph = createGraph(
        "compressor-response-graph",
        "Compressor input and output response"
    );
    const titles = Array.from(widget.querySelectorAll("h4"));
    titles[0]?.after(gateGraph);
    titles[1]?.after(compressorGraph);

    const update = () => {
        const gateThreshold = findControl(section, "DYNAMICS1_THRESHOLD");
        const compThreshold = findControl(section, "DYNAMICS2_THRESHOLD");
        const ratioControl = findControl(section, "DYNAMICS2_RATIO");
        const gateEnabled = widget.querySelector(
            ".dynamics-toggle-row .toggle-button"
        )?.classList.contains("active") ?? false;
        const compressorEnabled = widget.querySelectorAll(
            ".dynamics-toggle-row .toggle-button"
        )[1]?.classList.contains("active") ?? false;

        drawGateResponse(
            gateGraph,
            gateThreshold ? thresholdToDb(gateThreshold) : -40,
            gateEnabled
        );
        drawCompressorResponse(
            compressorGraph,
            compThreshold
                ? thresholdToDb(compThreshold)
                : -18,
            ratioControl ? ratioToValue(ratioControl) : 4,
            widget.querySelector(".dynamics-row .slider-container"),
            compressorEnabled
        );
    };

    const ratioControl = findControl(section, "DYNAMICS2_RATIO");
    const ratioRow = widget.querySelectorAll(".dynamics-toggle-row")[2];
    ratioRow?.addEventListener("click", () => {
        const ratioToggle = ratioRow.querySelector(".toggle-button");
        const input = ratioControl?.querySelector('input[type="range"]');
        if (input) input.value = ratioToggle?.classList.contains("active") ? "11" : "6";
        update();
    });
    section.querySelectorAll('[data-role^="DYNAMICS"]').forEach(control => {
        control.addEventListener("control-update", update);
    });
    section.addEventListener("pointermove", update);
    section.addEventListener("input", update);
    widget.addEventListener("input", update);
    widget.addEventListener("click", update);
    widget.addEventListener("pointermove", update);
    update();
}

function createGraph(className, label) {
    const svg = document.createElementNS(SVG_NS, "svg");
    svg.classList.add("detail-response-graph", className);
    svg.setAttribute("viewBox", `0 0 ${GRAPH_WIDTH} ${GRAPH_HEIGHT}`);
    svg.setAttribute("role", "img");
    svg.setAttribute("aria-label", label);
    return svg;
}

function preparePlot(svg, xTicks, yTicks, xLabel, yLabel) {
    svg.replaceChildren();
    const width = GRAPH_WIDTH - PLOT.left - PLOT.right;
    const height = GRAPH_HEIGHT - PLOT.top - PLOT.bottom;
    const group = createSvgElement("g");

    yTicks.forEach(value => {
        const y = plotY(value, yTicks[0], yTicks[yTicks.length - 1]);
        group.appendChild(line(PLOT.left, y, GRAPH_WIDTH - PLOT.right, y, "graph-grid"));
        addText(group, PLOT.left - 7, y + 3, String(value), "graph-tick", "end");
    });

    xTicks.forEach(({ position, label }) => {
        const x = PLOT.left + width * position;
        group.appendChild(line(x, PLOT.top, x, GRAPH_HEIGHT - PLOT.bottom, "graph-grid"));
        addText(group, x, GRAPH_HEIGHT - PLOT.bottom + 15, label, "graph-tick", "middle");
    });

    group.appendChild(line(
        PLOT.left,
        PLOT.top,
        PLOT.left,
        GRAPH_HEIGHT - PLOT.bottom,
        "graph-axis"
    ));
    group.appendChild(line(
        PLOT.left,
        GRAPH_HEIGHT - PLOT.bottom,
        GRAPH_WIDTH - PLOT.right,
        GRAPH_HEIGHT - PLOT.bottom,
        "graph-axis"
    ));
    addText(group, PLOT.left + width / 2, GRAPH_HEIGHT - 2, xLabel, "graph-label", "middle");
    addText(group, 11, PLOT.top + height / 2, yLabel, "graph-label", "middle")
        .setAttribute("transform", `rotate(-90 11 ${PLOT.top + height / 2})`);
    svg.appendChild(group);
    return group;
}

function drawEqResponse(svg, bands) {
    const group = preparePlot(
        svg,
        [
            { position: 0, label: "20" },
            { position: logFrequencyPosition(100), label: "100" },
            { position: logFrequencyPosition(1000), label: "1k" },
            { position: logFrequencyPosition(10000), label: "10k" },
            { position: 1, label: "20k" }
        ],
        [-18, -12, -6, 0, 6, 12, 18],
        "Frequency (Hz)",
        "Gain (dB)"
    );
    const points = [];
    const sampleCount = 300;

    for (let index = 0; index <= sampleCount; index++) {
        const frequency = EQ_MIN_FREQUENCY * (
            EQ_MAX_FREQUENCY / EQ_MIN_FREQUENCY
        ) ** (index / sampleCount);
        const gainDb = bands.reduce(
            (total, band) => total + biquadGainDb(frequency, band),
            0
        );
        points.push([
            PLOT.left + (GRAPH_WIDTH - PLOT.left - PLOT.right) * index / sampleCount,
            plotY(Math.max(-18, Math.min(18, gainDb)), -18, 18)
        ]);
    }

    group.appendChild(path(points, "eq-response-line"));
}

function drawGateResponse(svg, thresholdDb, enabled) {
    const group = preparePlot(
        svg,
        [
            { position: 0, label: "-60" },
            { position: 1 / 3, label: "-40" },
            { position: 2 / 3, label: "-20" },
            { position: 1, label: "0" }
        ],
        [-60, -40, -20, 0],
        "Input level (dB)",
        "Output (dB)"
    );
    const threshold = Math.max(-60, Math.min(0, thresholdDb));
    const points = [];

    for (let index = 0; index <= 120; index++) {
        const input = -60 + index / 2;
        let output = input;
        if (enabled && input < threshold) {
            output = Math.max(-60, threshold - 30 + (input - threshold) * 0.25);
        }
        points.push([
            PLOT.left + (GRAPH_WIDTH - PLOT.left - PLOT.right) * index / 120,
            plotY(output, -60, 0)
        ]);
    }
    group.appendChild(path(points, enabled ? "gate-response-line" : "graph-bypass-line"));
    group.appendChild(circle(
        PLOT.left + (GRAPH_WIDTH - PLOT.left - PLOT.right) * (threshold + 60) / 60,
        plotY(threshold, -60, 0),
        "graph-threshold-marker"
    ));
}

function drawCompressorResponse(svg, thresholdDb, ratio, slider, enabled) {
    const group = preparePlot(
        svg,
        [
            { position: 0, label: "-60" },
            { position: 1 / 3, label: "-40" },
            { position: 2 / 3, label: "-20" },
            { position: 1, label: "0" }
        ],
        [-60, -40, -20, 0],
        "Input level (dB)",
        "Output (dB)"
    );
    const backingValue = slider?.querySelector('input[type="range"]');
    const thresholdValue = Number(backingValue?.value);
    const threshold = Number.isFinite(thresholdValue)
        ? thresholdValue / 10
        : thresholdDb;
    const points = [];

    for (let index = 0; index <= 120; index++) {
        const input = -60 + index / 2;
        const output = !enabled || input <= threshold
            ? input
            : threshold + (input - threshold) / ratio;
        points.push([
            PLOT.left + (GRAPH_WIDTH - PLOT.left - PLOT.right) * index / 120,
            plotY(output, -60, 0)
        ]);
    }

    group.appendChild(path(
        points,
        enabled ? "compressor-response-line" : "graph-bypass-line"
    ));
    group.appendChild(circle(
        PLOT.left + (GRAPH_WIDTH - PLOT.left - PLOT.right) * (threshold + 60) / 60,
        plotY(threshold, -60, 0),
        "graph-threshold-marker"
    ));
}

function findControl(section, role) {
    return section.querySelector(`[data-role="${role}"]`);
}

function controlValue(control) {
    const input = control.querySelector('input[type="range"]');
    const value = Number(input?.value);
    return Number.isFinite(value) ? value : Number(control.controlMeta?.value);
}

function normalizedValue(control) {
    const value = controlValue(control);
    const min = Number(control.controlMeta?.min ?? control.dataset.min);
    const max = Number(control.controlMeta?.max ?? control.dataset.max);
    if (!Number.isFinite(value) || !Number.isFinite(min) || max <= min) return 0.5;
    return Math.max(0, Math.min(1, (value - min) / (max - min)));
}

function gainToDb(control, value = controlValue(control)) {
    const metadata = control.controlMeta;
    const unitMin = Number(metadata?.unitMin);
    const unitMax = Number(metadata?.unitMax);
    const min = Number(metadata?.min ?? control.dataset.min);
    const max = Number(metadata?.max ?? control.dataset.max);
    const normalized = Number.isFinite(value) && Number.isFinite(min) && max > min
        ? Math.max(0, Math.min(1, (value - min) / (max - min)))
        : 0.5;
    if (Number.isFinite(unitMin) && Number.isFinite(unitMax) && unitMax > unitMin) {
        return unitMin + normalized * (unitMax - unitMin);
    }
    return -18 + normalized * 36;
}

function frequencyToHz(control) {
    return EQ_MIN_FREQUENCY * (
        EQ_MAX_FREQUENCY / EQ_MIN_FREQUENCY
    ) ** normalizedValue(control);
}

function qToValue(control) {
    return 0.1 * (100 ** normalizedValue(control));
}

function thresholdToDb(control) {
    const value = controlValue(control);
    return Number.isFinite(value) ? value / 10 : -40;
}

function ratioToValue(control) {
    const index = Math.round(normalizedValue(control) * (COMPRESSOR_RATIOS.length - 1));
    const ratio = COMPRESSOR_RATIOS[index];
    return Number.isFinite(ratio) ? ratio : 20;
}

function biquadGainDb(frequency, band) {
    const sampleRate = 48000;
    const boundedFrequency = Math.max(1, Math.min(sampleRate / 2, band.frequencyHz));
    const amplitude = 10 ** (band.gainDb / 40);
    const omega = 2 * Math.PI * boundedFrequency / sampleRate;
    const cosine = Math.cos(omega);
    const alpha = Math.sin(omega) / (2 * Math.max(0.1, band.q));
    const a0 = 1 + alpha / amplitude;
    const b0 = (1 + alpha * amplitude) / a0;
    const b1 = (-2 * cosine) / a0;
    const b2 = (1 - alpha * amplitude) / a0;
    const a1 = (-2 * cosine) / a0;
    const a2 = (1 - alpha / amplitude) / a0;
    const angle = 2 * Math.PI * frequency / sampleRate;
    const realNumerator = b0 + b1 * Math.cos(angle) + b2 * Math.cos(2 * angle);
    const imaginaryNumerator = -b1 * Math.sin(angle) - b2 * Math.sin(2 * angle);
    const realDenominator = 1 + a1 * Math.cos(angle) + a2 * Math.cos(2 * angle);
    const imaginaryDenominator = -a1 * Math.sin(angle) - a2 * Math.sin(2 * angle);
    const magnitude = Math.hypot(realNumerator, imaginaryNumerator)
        / Math.max(1e-12, Math.hypot(realDenominator, imaginaryDenominator));
    return 20 * Math.log10(Math.max(1e-12, magnitude));
}

function logFrequencyPosition(frequency) {
    return Math.log(frequency / EQ_MIN_FREQUENCY)
        / Math.log(EQ_MAX_FREQUENCY / EQ_MIN_FREQUENCY);
}

function plotY(value, minimum, maximum) {
    return GRAPH_HEIGHT - PLOT.bottom
        - (value - minimum) / (maximum - minimum)
        * (GRAPH_HEIGHT - PLOT.top - PLOT.bottom);
}

function createSvgElement(tagName) {
    return document.createElementNS(SVG_NS, tagName);
}

function line(x1, y1, x2, y2, className) {
    const element = createSvgElement("line");
    element.setAttribute("x1", x1);
    element.setAttribute("y1", y1);
    element.setAttribute("x2", x2);
    element.setAttribute("y2", y2);
    element.setAttribute("class", className);
    return element;
}

function path(points, className) {
    const element = createSvgElement("path");
    element.setAttribute(
        "d",
        points.map(([x, y], index) => `${index ? "L" : "M"}${x.toFixed(2)},${y.toFixed(2)}`).join(" ")
    );
    element.setAttribute("class", className);
    return element;
}

function circle(cx, cy, className) {
    const element = createSvgElement("circle");
    element.setAttribute("cx", cx);
    element.setAttribute("cy", cy);
    element.setAttribute("r", "3");
    element.setAttribute("class", className);
    return element;
}

function addText(parent, x, y, value, className, anchor) {
    const element = createSvgElement("text");
    element.setAttribute("x", x);
    element.setAttribute("y", y);
    element.setAttribute("class", className);
    element.setAttribute("text-anchor", anchor);
    element.textContent = value;
    parent.appendChild(element);
    return element;
}
