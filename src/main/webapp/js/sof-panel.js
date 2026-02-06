export function buildSofPanelFromModel(model, handleSelection) {

    const panel = document.getElementById("sof-panel");
    if (!panel) return;

    panel.innerHTML = ""; // clear anything currently in the panel

    const sends = new Set();

    // -----------------------------------------------------
    // Scan controls for MIX/AUX sends from canonicalId
    // -----------------------------------------------------
    for (const c of model.controls) {

        if (!c || !c.canonicalId) continue;

        // canonicalId examples:
        //   "kInputAUX.kAUX1Level.0"
        //   "kInputToMix.kMix5Level.0"

        const match = c.canonicalId.match(/k(AUX|Mix)(\d+)Level/i);
        if (!match) continue;

        let type = match[1].toUpperCase(); // "AUX" or "MIX"
        const index = match[2];            // e.g., "1", "12"

        // -----------------------------------------------------
        // Yamaha 01V96i exposes MIX sends internally as AUX sends
        // but UI needs MIX1, MIX2, MIX3... in SOF mode.
        // -----------------------------------------------------
        if (type === "AUX") {
            type = "MIX";
        }

        sends.add(`${type}${index}`);
    }

    // -----------------------------------------------------
    // Sort numerically (MIX1, MIX2, MIX10)
    // -----------------------------------------------------
    const sorted = Array.from(sends).sort((a, b) => {
        const aNum = Number(a.replace(/\D+/g, ""));
        const bNum = Number(b.replace(/\D+/g, ""));
        return aNum - bNum;
    });

    // -----------------------------------------------------
    // Create SOF buttons (MIX1, MIX2, MIX3...)
    // -----------------------------------------------------
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

    // -----------------------------------------------------
    // INPUT button (normal strip view)
    // -----------------------------------------------------
    const offBtn = document.createElement("button");
    offBtn.className = "sof-btn off";
    offBtn.textContent = "INPUT";

    offBtn.addEventListener("click", () => {
        handleSelection(null);
    });

    panel.appendChild(offBtn);
}