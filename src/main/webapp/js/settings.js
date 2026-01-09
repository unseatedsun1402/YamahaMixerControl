import { WebSocketClient } from "./websocketClient.js";

console.log(">>> settings.js LOADED <<<");

// Create WS client using same endpoint as console UI
const wsClient = new WebSocketClient(`ws://${location.host}/MidiControl/endpoint`);
wsClient.connect();

wsClient.on("connected", () => {
  console.log("[Settings] Connected, requesting device list...");
  wsClient.requestMidiDevices();
});

wsClient.on("midi-device-list", (devices) => {
  console.log("[Settings] Received device list:", devices);
  populateDeviceDropdowns(devices);
});

document.getElementById("apply-settings").addEventListener("click", () => {

  const inputDeviceId = parseInt(document.getElementById("midi-input-device").value, 10);
  const outputDeviceId = parseInt(document.getElementById("midi-output-device").value, 10);

  const settings = {
    inputDeviceId,
    outputDeviceId

    // Future settings:
    // inputChannel: document.getElementById("input-channel").value,
    // outputChannel: parseInt(document.getElementById("output-channel").value, 10),
    // enableNRPN: document.getElementById("enable-nrpn").checked,
    // enableSYSEX: document.getElementById("enable-sysex").checked,
    // enableCC: document.getElementById("enable-cc").checked,
    // debugLogging: document.getElementById("debug-logging").checked,
    // showRaw: document.getElementById("show-raw").checked,
    // showCanonical: document.getElementById("show-canonical").checked
  };

  wsClient.applyMidiSettings(settings);
  showStatus("Settings applied");
});

document.getElementById("rescan-devices").addEventListener("click", () => {
  wsClient.requestMidiDevices();
});

function populateDeviceDropdowns(devices) {
  const inputSelect = document.getElementById("midi-input-device");
  const outputSelect = document.getElementById("midi-output-device");

  inputSelect.innerHTML = "";
  outputSelect.innerHTML = "";

  devices.forEach(d => {
    const opt = document.createElement("option");
    opt.value = d.id;
    opt.textContent = `${d.name} (${d.vendor})`;

    if (d.canInput) inputSelect.appendChild(opt.cloneNode(true));
    if (d.canOutput) outputSelect.appendChild(opt.cloneNode(true));
  });
}

function showStatus(text) {
  let el = document.getElementById("settings-status");
  if (!el) {
    el = document.createElement("div");
    el.id = "settings-status";
    el.style.position = "fixed";
    el.style.bottom = "20px";
    el.style.right = "20px";
    el.style.background = "#4da3ff";
    el.style.color = "#000";
    el.style.padding = "10px 16px";
    el.style.borderRadius = "6px";
    el.style.fontWeight = "bold";
    el.style.opacity = "0";
    el.style.transition = "opacity 0.3s";
    document.body.appendChild(el);
  }

  el.textContent = text;
  el.style.opacity = 1;
  setTimeout(() => el.style.opacity = 0, 2000);
}