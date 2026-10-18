import {
    renderHorizontalSlider,
    updateHorizontalSlider
} from "./sliderHorizontal.js";

const MIN_EQ_FREQUENCY = 20;
const MAX_EQ_FREQUENCY = 20000;

const EQ_PRESETS = [
    {
        id: "vocal",
        label: "Vocal — forward and airy",
        bands: [
            { gainDb: 5, frequencyHz: 120, q: 0.3 },
            { gainDb: -5, frequencyHz: 300, q: 0.55 },
            { gainDb: 8, frequencyHz: 3500, q: 0.62 },
            { gainDb: 5, frequencyHz: 10000, q: 0.35 }
        ]
    },
    {
        id: "electric-guitar",
        label: "Electric guitar — bite and body",
        bands: [
            { gainDb: 4, frequencyHz: 110, q: 0.35 },
            { gainDb: -5, frequencyHz: 350, q: 0.58 },
            { gainDb: 7, frequencyHz: 2800, q: 0.62 },
            { gainDb: -2, frequencyHz: 8000, q: 0.4 }
        ]
    },
    {
        id: "acoustic-guitar",
        label: "Acoustic guitar — body and sparkle",
        bands: [
            { gainDb: 5, frequencyHz: 100, q: 0.35 },
            { gainDb: -4, frequencyHz: 250, q: 0.5 },
            { gainDb: 5, frequencyHz: 3200, q: 0.48 },
            { gainDb: 6, frequencyHz: 10000, q: 0.3 }
        ]
    },
    {
        id: "bass",
        label: "Bass — weight and definition",
        bands: [
            { gainDb: 7, frequencyHz: 70, q: 0.38 },
            { gainDb: -5, frequencyHz: 250, q: 0.58 },
            { gainDb: 5, frequencyHz: 800, q: 0.48 },
            { gainDb: 3, frequencyHz: 3000, q: 0.4 }
        ]
    },
    {
        id: "punchy-kick",
        label: "Kick — punchy",
        bands: [
            { gainDb: 9, frequencyHz: 60, q: 0.42 },
            { gainDb: -7, frequencyHz: 250, q: 0.68 },
            { gainDb: 7, frequencyHz: 3500, q: 0.68 },
            { gainDb: -2, frequencyHz: 9000, q: 0.45 }
        ]
    },
    {
        id: "thick-wub-kick",
        label: "Kick — thick wub",
        bands: [
            { gainDb: 10, frequencyHz: 50, q: 0.32 },
            { gainDb: 6, frequencyHz: 100, q: 0.45 },
            { gainDb: -6, frequencyHz: 350, q: 0.6 },
            { gainDb: -4, frequencyHz: 4000, q: 0.52 }
        ]
    },
    {
        id: "high-tilt",
        label: "Tilt — bright",
        bands: [
            { gainDb: -6, frequencyHz: 100, q: 0.38 },
            { gainDb: -3, frequencyHz: 500, q: 0.42 },
            { gainDb: 3, frequencyHz: 3000, q: 0.42 },
            { gainDb: 7, frequencyHz: 10000, q: 0.38 }
        ]
    },
    {
        id: "low-tilt",
        label: "Tilt — warm",
        bands: [
            { gainDb: 7, frequencyHz: 100, q: 0.38 },
            { gainDb: 3, frequencyHz: 500, q: 0.42 },
            { gainDb: -3, frequencyHz: 3000, q: 0.42 },
            { gainDb: -6, frequencyHz: 10000, q: 0.38 }
        ]
    }
];

const BAND_LABELS = ["LOW", "LMID", "HMID", "HIGH"];

export function createEQWidget(section) {
    if (section.querySelector(".eq-widget")) {
        return;
    }

    const bands = BAND_LABELS.map((label, index) => {
        const bandNumber = index + 1;
        return {
            label,
            gain: section.querySelector(
                `[data-role="INPUT_EQ${bandNumber}_GAIN"]`
            ),
            frequency: section.querySelector(
                `[data-role="INPUT_EQ${bandNumber}_FREQUENCY"]`
            ),
            q: section.querySelector(
                `[data-role="INPUT_EQ${bandNumber}_Q"]`
            )
        };
    });

    const visibleBands = bands.filter(band => band.gain);
    if (visibleBands.length === 0) {
        return;
    }

    bands.forEach(band => {
        [band.gain, band.frequency, band.q]
            .filter(Boolean)
            .forEach(control => {
                control.style.display = "none";
            });
    });

    const widget = document.createElement("div");
    widget.className = "eq-widget";

    const title = document.createElement("h4");
    title.textContent = "EQ";
    widget.appendChild(title);

    const presetLabel = document.createElement("label");
    presetLabel.className = "eq-preset-label";
    presetLabel.textContent = "Tone shape";

    const presetSelect = document.createElement("select");
    presetSelect.className = "eq-preset-select";
    presetSelect.setAttribute("aria-label", "EQ tone shape preset");

    const placeholder = document.createElement("option");
    placeholder.value = "";
    placeholder.textContent = "Manual / current";
    presetSelect.appendChild(placeholder);

    EQ_PRESETS.forEach(preset => {
        const option = document.createElement("option");
        option.value = preset.id;
        option.textContent = preset.label;
        presetSelect.appendChild(option);
    });

    presetLabel.appendChild(presetSelect);
    widget.appendChild(presetLabel);

    const status = document.createElement("div");
    status.className = "eq-preset-status";
    status.setAttribute("aria-live", "polite");

    bands.forEach(band => {
        if (!band.gain) {
            return;
        }

        const row = document.createElement("div");
        row.className = "eq-row";

        const label = document.createElement("label");
        label.textContent = band.label;

        const slider = renderHorizontalSlider({
            min: readRangeValue(band.gain, "min", -180),
            max: readRangeValue(band.gain, "max", 180),
            value: readRangeValue(band.gain, "value", 0),
            readOnly: false,
            canonicalId: band.gain.dataset.canonicalId
        });

        band.gain.addEventListener("control-update", event => {
            updateHorizontalSlider(slider, event.detail.value);
        });

        slider.addEventListener("pointerdown", () => {
            presetSelect.value = "";
            status.textContent = "";
        });

        row.appendChild(label);
        row.appendChild(slider);
        widget.appendChild(row);
    });

    presetSelect.addEventListener("change", () => {
        const preset = EQ_PRESETS.find(item => item.id === presetSelect.value);
        if (preset) {
            applyPreset(bands, preset, status);
        } else {
            status.textContent = "";
        }
    });

    widget.appendChild(status);
    section.appendChild(widget);
}

function applyPreset(bands, preset, status) {
    if (!window.wsClient || typeof window.wsClient.sendControlChange !== "function") {
        console.error("Cannot apply EQ preset: control connection is unavailable.");
        status.textContent = "Preset could not be sent: control connection unavailable.";
        return;
    }

    status.textContent = "";
    let skippedParameters = false;

    bands.forEach((band, index) => {
        const values = preset.bands[index];
        const gainValue = unitToControlValue(band.gain, values.gainDb, "dB");
        const frequencyValue = frequencyToControlValue(band.frequency, values.frequencyHz);
        const qValue = normalizedToControlValue(band.q, values.q);

        if (band.gain && gainValue == null) {
            skippedParameters = true;
        } else if (!sendControlValue(band.gain, gainValue)) {
            skippedParameters = true;
        }

        if (band.frequency && frequencyValue == null) {
            skippedParameters = true;
        } else if (!sendControlValue(band.frequency, frequencyValue)) {
            skippedParameters = true;
        }

        if (band.q && qValue == null) {
            skippedParameters = true;
        } else if (!sendControlValue(band.q, qValue)) {
            skippedParameters = true;
        }

        if (band.gain && (!band.frequency || !band.q)) {
            skippedParameters = true;
        }
    });

    if (skippedParameters) {
        status.textContent = "Some EQ parameters could not be calibrated or are unavailable; available controls were set.";
    }
}

function sendControlValue(control, targetValue) {
    if (!control) {
        return true;
    }

    if (!Number.isFinite(targetValue)) {
        return false;
    }

    const rangeInput = control.querySelector('input[type="range"]');
    if (!rangeInput) {
        console.error("Cannot apply EQ preset: backing control has no range input.", control);
        return false;
    }

    const min = Number(rangeInput.min);
    const max = Number(rangeInput.max);
    const value = Math.round(Math.max(min, Math.min(max, targetValue)));

    updateHorizontalSlider(control, value);
    window.wsClient.sendControlChange(control.dataset.canonicalId, value);
    return true;
}

function unitToControlValue(control, targetValue, unit) {
    if (!control) {
        return null;
    }

    const metadata = control.controlMeta;
    if (!metadata || metadata.unit !== unit
            || !Number.isFinite(metadata.unitMin)
            || !Number.isFinite(metadata.unitMax)
            || metadata.unitMin === metadata.unitMax) {
        return null;
    }

    const rangeInput = control.querySelector('input[type="range"]');
    if (!rangeInput) {
        return null;
    }

    const min = Number(rangeInput.min);
    const max = Number(rangeInput.max);
    const boundedValue = Math.max(
        metadata.unitMin,
        Math.min(metadata.unitMax, targetValue)
    );
    const position = (boundedValue - metadata.unitMin)
        / (metadata.unitMax - metadata.unitMin);

    return min + position * (max - min);
}

function frequencyToControlValue(control, frequencyHz) {
    if (!control) {
        return null;
    }

    const rangeInput = control.querySelector('input[type="range"]');
    if (!rangeInput) {
        return null;
    }

    const min = Number(rangeInput.min);
    const max = Number(rangeInput.max);
    const boundedFrequency = Math.max(
        MIN_EQ_FREQUENCY,
        Math.min(MAX_EQ_FREQUENCY, frequencyHz)
    );
    const position = Math.log(boundedFrequency / MIN_EQ_FREQUENCY)
        / Math.log(MAX_EQ_FREQUENCY / MIN_EQ_FREQUENCY);

    return min + position * (max - min);
}

function normalizedToControlValue(control, position) {
    if (!control) {
        return null;
    }

    const rangeInput = control.querySelector('input[type="range"]');
    if (!rangeInput) {
        return null;
    }

    const min = Number(rangeInput.min);
    const max = Number(rangeInput.max);
    const boundedPosition = Math.max(0, Math.min(1, position));
    return min + boundedPosition * (max - min);
}

function readRangeValue(control, property, fallback) {
    const input = control.querySelector('input[type="range"]');
    const value = input ? Number(input[property]) : NaN;
    return Number.isFinite(value) ? value : fallback;
}
