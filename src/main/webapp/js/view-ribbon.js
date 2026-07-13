export function buildViewRibbonFromModel(model, handleSelection) {

    const panel = document.getElementById("view-ribbon");
    if (!panel) return;

    panel.innerHTML = ""; // clear anything currently in the panel

    const sends = new Set();

    for (const c of model.controls) {

        if (!c || !c.canonicalId) continue;


        const match = c.canonicalId.match(/k(AUX|Mix)(\d+)Level/i);
        if (!match) continue;

        let type = match[1].toUpperCase();
        const index = match[2];

        if (type === "AUX") {
            type = "MIX";
        }

        sends.add(`${type}${index}`);
    }

    const sorted = Array.from(sends).sort((a, b) => {
        const aNum = Number(a.replace(/\D+/g, ""));
        const bNum = Number(b.replace(/\D+/g, ""));
        return aNum - bNum;
    });

    for (const sendId of sorted) {
        const btn = document.createElement("button");
        btn.className = "sof-btn";
        btn.textContent = sendId;

        btn.addEventListener("click", () => {
            // server expects "mixN"
            handleSelection(sendId.toLowerCase());
        });

        panel.appendChild(btn);
    }

    const offBtn = document.createElement("button");
    offBtn.className = "sof-btn off";
    offBtn.textContent = "INPUT";

    offBtn.addEventListener("click", () => {
        handleSelection(null);
    });

    panel.appendChild(offBtn);

    const editBtn = document.createElement("button");
    editBtn.className = "edit-btn off";
    editBtn.textContent = "EDIT";

    editBtn.addEventListener("click", () => {
        handleSelection("EDIT");
    });

    panel.appendChild(editBtn);
}