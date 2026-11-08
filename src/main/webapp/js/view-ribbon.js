export function buildViewRibbonFromModel(
    model,
    handleSofSelection
) {

    const panel =
        document.getElementById(
            "view-ribbon"
        );

    if (!panel) return;

    panel.innerHTML = "";

    const deskStatusElement = document.createElement("div");
    deskStatusElement.id = "desk-status";
    deskStatusElement.className = "desk-dead";
    deskStatusElement.textContent = "Waiting for desk status…";
    deskStatusElement.setAttribute("role", "status");
    deskStatusElement.setAttribute("aria-live", "polite");
    deskStatusElement.setAttribute(
        "aria-label",
        "Waiting for desk status"
    );

    panel.appendChild(deskStatusElement);

    const settingsLink = document.createElement("a");
    settingsLink.className = "sof-btn soft-key page-nav-link";
    settingsLink.href = "/MidiControl/settings.html";
    settingsLink.textContent = "SETTINGS";
    panel.appendChild(settingsLink);

    const metersLink = document.createElement("a");
    metersLink.className = "sof-btn soft-key page-nav-link";
    metersLink.href = "/MidiControl/meters.html";
    metersLink.textContent = "METERS";
    panel.appendChild(metersLink);

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

        const sendNumber = Number(sendId.replace(/\D+/g, ""));
        const sendBank = Math.floor(((sendNumber - 1) % 16) / 4) + 1;
        btn.className = `sof-btn soft-key sof-send-btn sof-send-bank-${sendBank}`;
        btn.textContent = sendId;
        btn.setAttribute("aria-pressed", "false");
        btn.dataset.send = sendId.toLowerCase();

        btn.addEventListener(
            "click",
            () => {
                panel.querySelectorAll(".sof-send-btn").forEach(sendButton => {
                    const isActive = sendButton === btn;
                    sendButton.classList.toggle("active", isActive);
                    sendButton.setAttribute("aria-pressed", String(isActive));
                });
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
        "sof-btn soft-key edit-btn off";

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

export function buildPatchRibbon(selectBank) {

    const panel = document.getElementById("patch-ribbon");
    if (!panel) return;

    panel.innerHTML = "";

    for (const bank of window.patchBanks) {

        const btn = document.createElement("button");
        btn.className = "sof-btn off";
        btn.textContent = bank.label;

        btn.addEventListener("click", () => {
            selectBank(bank.id);
        });

        panel.appendChild(btn);
    }
}
