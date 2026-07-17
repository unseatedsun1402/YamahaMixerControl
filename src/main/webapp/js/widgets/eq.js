import {
    renderHorizontalSlider,
    updateHorizontalSlider
} from "./sliderHorizontal.js";

export function createEQWidget(section) {

    const eq1g = section.querySelector(
        '[data-role="INPUT_EQ1_GAIN"]'
    );

    const eq2g = section.querySelector(
        '[data-role="INPUT_EQ2_GAIN"]'
    );

    const eq3g = section.querySelector(
        '[data-role="INPUT_EQ3_GAIN"]'
    );

    const eq4g = section.querySelector(
        '[data-role="INPUT_EQ4_GAIN"]'
    );

    if (!eq1g) {
        return;
    }

    if (section.querySelector(".eq-widget")) {
        return;
    }

    [eq1g, eq2g, eq3g, eq4g]
        .filter(Boolean)
        .forEach(control => {
            control.style.display = "none";
        });

    const widget = document.createElement("div");
    widget.className = "eq-widget";

    const title = document.createElement("h4");
    title.textContent = "EQ";

    widget.appendChild(title);

    const bands = [
        { label: "LOW", control: eq1g },
        { label: "LMID", control: eq2g },
        { label: "HMID", control: eq3g },
        { label: "HIGH", control: eq4g }
    ];

    bands.forEach(band => {

        if (!band.control) {
            return;
        }

        const backingSlider =
            band.control.querySelector(
                'input[type="range"]'
            );

        const row = document.createElement("div");
        row.className = "eq-row";

        const label = document.createElement("label");
        label.textContent = band.label;

        const slider = renderHorizontalSlider({

            min:
                backingSlider
                    ? Number(backingSlider.min)
                    : -180,

            max:
                backingSlider
                    ? Number(backingSlider.max)
                    : 180,

            value:
                backingSlider
                    ? Number(backingSlider.value)
                    : 0,

            readOnly: false,

            canonicalId:
                band.control.dataset.canonicalId
        });

        band.control.addEventListener(
            "control-update",
            e => {

                updateHorizontalSlider(
                    slider,
                    e.detail.value
                );
            }
        );

        row.appendChild(label);
        row.appendChild(slider);

        widget.appendChild(row);
    });

    section.appendChild(widget);
}