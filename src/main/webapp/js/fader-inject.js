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

function setFaderMarker(faderElement, color) {
  const markerClasses = ['marker-red', 'marker-blue', 'marker-green'];
  faderElement.classList.remove(...markerClasses);
  faderElement.classList.add(`marker-${color}`);
}

// Usage
function colorFaderMarkers() {
  document.querySelectorAll('.fader').forEach(fader => {
    const msb = parseInt(fader.dataset.msb, 10);
    if (msb >= 0x30 && msb <= 0x3F) {
      setFaderMarker(fader, 'red');
    } else if (msb >= 0x40 && msb <= 0x4F) {
      setFaderMarker(fader, 'blue');
    } else if (msb >= 0x50 && msb <= 0x5F) {
      setFaderMarker(fader, 'green');
    }
  });
}

colorFaderMarkers();