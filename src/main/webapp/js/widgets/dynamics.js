export function createDynamicsWidget(section) {

    const dyn1 = section.querySelector(
        '[data-role="DYNAMICS1_ON"]'
    );

    const dyn2 = section.querySelector(
        '[data-role="DYNAMICS2_ON"]'
    );

    const ratio = section.querySelector(
        '[data-role="DYNAMICS2_RATIO"]'
    );

    const threshold = section.querySelector(
        '[data-role="DYNAMICS2_THRESHOLD"]'
    );

    if (!dyn2) {
        return;
    }

    if (section.querySelector(".dynamics-widget")) {
        return;
    }

    [dyn1, dyn2, ratio, threshold]
        .filter(Boolean)
        .forEach(control => {
            control.style.display = "none";
        });

    const widget = document.createElement("div");
    widget.className = "dynamics-widget";

    // GATE

    const gateTitle = document.createElement("h4");
    gateTitle.textContent = "Gate";

    const gateEnable = document.createElement("button");
    gateEnable.className = "toggle-button dynamics";
    gateEnable.textContent = "Gate On";

    gateEnable.addEventListener("click", () => {

        const enabled =
            !gateEnable.classList.contains("active");

        gateEnable.classList.toggle(
            "active",
            enabled
        );

        if (dyn1) {
            window.wsClient?.sendControlChange(
                dyn1.dataset.canonicalId,
                enabled ? 127 : 0
            );
        }
    });

    // COMPRESSOR

    const compressorTitle =
        document.createElement("h4");

    compressorTitle.textContent =
        "Compressor";

    const compEnable = document.createElement("button");

    compEnable.className =
        "toggle-button dynamics";

    compEnable.textContent = "Comp On";

    compEnable.addEventListener("click", () => {

        const enabled =
            !compEnable.classList.contains("active");

        compEnable.classList.toggle(
            "active",
            enabled
        );

        window.wsClient?.sendControlChange(
            dyn2.dataset.canonicalId,
            enabled ? 127 : 0
        );
    });

    // SOFT / HARD RATIO

    const ratioInput =
        ratio?.querySelector(
            'input[type="range"]'
        );

    const currentRatio =
        ratioInput
            ? Number(ratioInput.value)
            : 6;

    const ratioToggle =
        document.createElement("button");

    ratioToggle.className =
        "toggle-button dynamics";

    const hard =
        currentRatio >= 11;

    ratioToggle.textContent =
        hard ? "Hard Ratio" : "Soft Ratio";

    ratioToggle.classList.toggle(
        "active",
        hard
    );

    ratioToggle.addEventListener("click", () => {

        const hardSelected =
            !ratioToggle.classList.contains(
                "active"
            );

        ratioToggle.classList.toggle(
            "active",
            hardSelected
        );

        ratioToggle.textContent =
            hardSelected
                ? "Hard Ratio"
                : "Soft Ratio";

        if (!ratio) {
            return;
        }

        window.wsClient?.sendControlChange(
            ratio.dataset.canonicalId,
            hardSelected ? 11 : 6
        );
    });

    // THRESHOLD

    const thresholdLabel =
        document.createElement("label");

    thresholdLabel.textContent =
        "Thresh";

    const thresholdSlider =
        document.createElement("input");

    thresholdSlider.type =
        "range";
    
    // Backing

    const backingGate =
    dyn1?.querySelector(".toggle-button");

    if (
        backingGate?.classList.contains(
            "active"
        )
    ) {
        gateEnable.classList.add("active");
    }

    const backingComp =
        dyn2?.querySelector(".toggle-button");

    if (
        backingComp?.classList.contains(
            "active"
        )
    ) {
        compEnable.classList.add("active");
    }

    const backingThreshold =
        threshold?.querySelector(
            'input[type="range"]'
        );

    if (backingThreshold) {

        thresholdSlider.min =
            backingThreshold.min;

        thresholdSlider.max =
            backingThreshold.max;

        thresholdSlider.value =
            backingThreshold.value;

    } else {

        thresholdSlider.min = 0;
        thresholdSlider.max = 127;
        thresholdSlider.value = 64;
    }

    thresholdSlider.addEventListener(
        "input",
        () => {

            if (!threshold) {
                return;
            }

            window.wsClient?.sendControlChange(
                threshold.dataset.canonicalId,
                Number(
                    thresholdSlider.value
                )
            );
        }
    );

    dyn1?.addEventListener(
        "control-update",
        e => {

            gateEnable.classList.toggle(
                "active",
                e.detail.value > 0
            );
        }
    );

    dyn2?.addEventListener(
        "control-update",
        e => {

            compEnable.classList.toggle(
                "active",
                e.detail.value > 0
            );
        }
    );

    ratio?.addEventListener(
        "control-update",
        e => {

            const hardMode =
                Number(e.detail.value) >= 11;

            ratioToggle.classList.toggle(
                "active",
                hardMode
            );

            ratioToggle.textContent =
                hardMode
                    ? "Hard Ratio"
                    : "Soft Ratio";
        }
    );

    threshold?.addEventListener(
        "control-update",
        e => {

            thresholdSlider.value =
                e.detail.value;
        }
    );

    //
    // LAYOUT
    //

    const gateRow =
        document.createElement("div");

    gateRow.className =
        "dynamics-toggle-row";

    gateRow.appendChild(gateEnable);

    const compRow =
        document.createElement("div");

    compRow.className =
        "dynamics-toggle-row";

    compRow.appendChild(compEnable);

    const ratioRow =
        document.createElement("div");

    ratioRow.className =
        "dynamics-toggle-row";

    ratioRow.appendChild(ratioToggle);

    const thresholdRow =
        document.createElement("div");

    thresholdRow.className =
        "dynamics-row";

    thresholdRow.append(
        thresholdLabel,
        thresholdSlider
    );

    widget.appendChild(gateTitle);
    widget.appendChild(gateRow);

    widget.appendChild(compressorTitle);
    widget.appendChild(compRow);

    widget.appendChild(ratioRow);
    widget.appendChild(thresholdRow);

    section.appendChild(widget);
}