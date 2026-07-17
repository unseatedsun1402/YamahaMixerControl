export function buildViewRibbonFromModel(
    model,
    handleSofSelection,
    selectInputMode,
    selectOutputMode
) {

    const panel =
        document.getElementById(
            "view-ribbon"
        );

    if (!panel) return;

    panel.innerHTML = "";

    // INPUT BUTTON

    const inputBtn =
        document.createElement(
            "button"
        );

    inputBtn.className =
        "sof-btn off";

    inputBtn.textContent =
        "INPUT";

    inputBtn.addEventListener(
        "click",
        () => {
            selectInputMode();
        }
    );

    panel.appendChild(
        inputBtn
    );

    // OUTPUT BUTTON

    const outputBtn =
        document.createElement(
            "button"
        );

    outputBtn.className =
        "sof-btn off";

    outputBtn.textContent =
        "OUTPUT";

    outputBtn.addEventListener(
        "click",
        () => {
            selectOutputMode();
        }
    );

    panel.appendChild(
        outputBtn
    );

    // SOF BUTTONS

    const sends = new Set();

    for (const c of model.controls) {

        if (!c?.canonicalId) {
            continue;
        }

        const match =
            c.canonicalId.match(
                /k(AUX|Mix)(\d+)Level/i
            );

        if (!match) {
            continue;
        }

        let type =
            match[1].toUpperCase();

        const index =
            match[2];

        if (type === "AUX") {
            type = "MIX";
        }

        sends.add(
            `${type}${index}`
        );
    }

    const sorted =
        Array.from(sends).sort(
            (a, b) => {

                const aNum =
                    Number(
                        a.replace(
                            /\D+/g,
                            ""
                        )
                    );

                const bNum =
                    Number(
                        b.replace(
                            /\D+/g,
                            ""
                        )
                    );

                return aNum - bNum;
            }
        );

    for (const sendId of sorted) {

        const btn =
            document.createElement(
                "button"
            );

        btn.className =
            "sof-btn";

        btn.textContent =
            sendId;

        btn.addEventListener(
            "click",
            () => {
                handleSofSelection(
                    sendId.toLowerCase()
                );
            }
        );

        panel.appendChild(
            btn
        );
    }

    // EDIT
    const editBtn =
        document.createElement(
            "button"
        );

    editBtn.className =
        "edit-btn off";

    editBtn.textContent =
        "EDIT";

    editBtn.addEventListener(
        "click",
        () => {
            handleSofSelection(
                "EDIT"
            );
        }
    );

    panel.appendChild(
        editBtn
    );
}