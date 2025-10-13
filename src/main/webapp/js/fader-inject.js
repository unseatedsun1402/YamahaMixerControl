const address = 0x0566;
const inputmsb = Math.floor(address / 128); // 10
const inputlsb = address % 128;             // 102

function splitNrpn(hexAddress) {
  const address = parseInt(hexAddress, 16);
  return {
    msb: Math.floor(address / 128),
    lsb: address % 128
  };
}

function getInputOnAddress(index) {
  if (index < 1 || index > 56) return null;
  return {
    msb: INPUT_ON_BASE.msb,
    lsb: INPUT_ON_BASE.lsb + (index - 1)
  };
}

function injectOutputFaders() {
  document.querySelectorAll('.output-fader').forEach(div => {
    const msb = parseInt(div.dataset.fadermsb, 10);
    const lsb = parseInt(div.dataset.faderlsb, 10);
    const label = div.dataset.label;
    const channelIndex = parseInt(div.dataset.channelindex, 10);

    const fader = document.createElement("input");
    fader.type = "range";
    fader.min = 0;
    fader.max = 127;
    fader.value = 80;
    fader.id = `output-fader-${channelIndex}`;
    fader.className = "fader";
    fader.dataset.msb = msb;
    fader.dataset.lsb = lsb;

    const labelEl = document.createElement("label");
    labelEl.textContent = label;
    labelEl.htmlFor = fader.id;

    const valueEl = document.createElement("div");
    valueEl.className = "fader-value";
    valueEl.dataset.msb = msb;
    valueEl.dataset.lsb = lsb;
    valueEl.textContent = `${faderDbMap[fader.value] || fader.value} dB`;

    fader.addEventListener("input", () => {
      sendMessage(fader);
    });

    div.appendChild(labelEl);
    div.appendChild(fader);
    div.appendChild(valueEl);
  });
  injectOutputFaderToggle();
}

function injectOutputFaderToggle(){
    document.getElementById("toggle-output-panel").addEventListener("click", () => {
    const panel = document.getElementById("output-fader-panel");
    panel.classList.toggle("hidden");
    });
}

function toggleMute(btn) {
  btn.classList.toggle("mute-active");

  const msb = parseInt(btn.dataset.msb, 10);
  const lsb = parseInt(btn.dataset.lsb, 10);
  const value = btn.classList.contains("mute-active") ? 0 : 127;

  const message = JSON.stringify({ id: btn.id, param: 0, value, msb, lsb });
  Client.sendMessage(message);
  console.info("Mute toggled:", message);
}

function toggleSolo(btn) {
  document.querySelectorAll(".solo").forEach(b => b.classList.remove("solo-active"));
  btn.classList.add("solo-active");

  const msb = parseInt(btn.dataset.msb, 10);
  const lsb = parseInt(btn.dataset.lsb, 10);
  const value = 127;

  // const message = JSON.stringify({ id: btn.id, param: 0, value, msb, lsb });
  // Client.sendMessage(message);
  console.info("Solo activated:", message);
}

