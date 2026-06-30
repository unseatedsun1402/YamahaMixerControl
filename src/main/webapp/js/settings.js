import { WebSocketClient } from "./websocketClient.js";

console.log(">>> settings.js LOADED <<<");

const ws = new WebSocketClient(`ws://${location.host}/MidiControl/endpoint`);
const SERVER_LOG_LIMIT = 50;
const serverLogBuffer = [];

const serverLog = document.getElementById("server-log");

ws.connect();

ws.on("connected", () => {
  console.log("[Settings] Connected, requesting device list...");
  ws.requestMidiDevices();
  ws.setSessionType("settings");
  console.info(`[Settings] Registering session type ${ws.sessionType}`);
});

ws.on("telemetry", updateTelemetry);
ws.on("server-event", handleServerEvent)
ws.on("REGISTRY_CHANGED", handleRegistryChanged);

ws.on("midi-device-list", (devices) => {
  console.log("[Settings] Received device list:", devices);
  populateDeviceDropdowns(devices);
});

document.addEventListener("change", function (e) {
    if (!e.target.classList.contains("profile-toggle")) return;

    const group = e.target.getAttribute("data-group");
    const all = document.querySelectorAll('.profile-toggle[data-group="' + group + '"]');

    all.forEach(x => {
        if (x !== e.target) x.checked = false;
    });
});


document.getElementById("apply-settings").addEventListener("click", () => {
  const inputDeviceId = parseInt(document.getElementById("midi-input-device").value, 10);
  const outputDeviceId = parseInt(document.getElementById("midi-output-device").value, 10);
  const consoleType = document.getElementById("console-type").value;

  const settings = {
    inputDeviceId,
    outputDeviceId,
    consoleType,
    // Future settings:
    // inputChannel: document.getElementById("input-channel").value,
    // outputChannel: parseInt(document.getElementById("output-channel").value, 10),
    safeprofile: document.getElementById("safe-profile").checked,
    mainprofile: document.getElementById("main-profile").checked,
    highprofile: document.getElementById("high-profile").checked
    // debugLogging: document.getElementById("debug-logging").checked,
    // showRaw: document.getElementById("show-raw").checked,
    // showCanonical: document.getElementById("show-canonical").checked
  };

  ws.applyMidiSettings(settings);
  showStatus("Settings applied");
});

document.getElementById("save-settings").addEventListener("click", () => {
  const inputDeviceId = parseInt(document.getElementById("midi-input-device").value, 10);
  const outputDeviceId = parseInt(document.getElementById("midi-output-device").value, 10);
  const consoleType = document.getElementById("console-type").value;

  const settings = {
    inputDeviceId,
    outputDeviceId,
    consoleType
  };

  ws.saveMidiSettings(settings);
  showStatus("Settings saved");
});

document.getElementById("rescan-devices").addEventListener("click", () => {
  ws.requestMidiDevices();
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

function updateTelemetry(data) {
    document.getElementById("telemetry-in").textContent =
        data.averagein + " B/s";

    document.getElementById("telemetry-out").textContent =
        data.averageout + " B/s";

    document.getElementById("telemetry-combined").textContent =
        data.averagecombined + " B/s";

    // Transport pressure (bytes outstanding)
    document.getElementById("telemetry-inflight").textContent =
        data.inflight + " bytes";

    // Damage / loss indicator
    document.getElementById("telemetry-dropped").textContent =
        data.dropped;

    // Internal queue headroom
    document.getElementById("remaining-capacity").textContent =
        data.remainingcapacity;

    // Rehydration pressure (if present)
    const inflightReqEl = document.getElementById("telemetry-inflight-requests");
    if (inflightReqEl && data.inflightTransactions !== undefined) {
        inflightReqEl.textContent = data.inflightTransactions;
    }

    const timeoutReqEl = document.getElementById("telemetry-timeout-requests");
    if (timeoutReqEl && data.timedOutTransactions !== undefined) {
        timeoutReqEl.textContent = data.timedOutTransactions;
    }

    const rttEl = document.getElementById("telemetry-rehydration-rtt");
    if (rttEl && data.rehydrationRttMs !== undefined && data.rehydrationRttMs >= 0) {
        rttEl.textContent = data.rehydrationRttMs + " ms";
    }

    appendTelemetryLog(data);
}

function appendTelemetryLog(data) {
    const log = document.getElementById("telemetry-log");
    const line = document.createElement("div");

    const t = new Date(data.timestamp * 1000).toLocaleTimeString();

    line.textContent =
        `[${t}] ` +
        `In=${data.averagein}B/s ` +
        `Out=${data.averageout}B/s ` +
        `Total=${data.averagecombined}B/s ` +
        `BufPressure=${data.inflight}B ` +
        `QueueFree=${data.remainingcapacity}`;

    // Optional rehydration signals (only if present)
    if (data.inflightTransactions !== undefined) {
        line.textContent += ` ReqPressure=${data.inflightTransactions}`;
    }

    if (data.timedOutTransactions !== undefined && data.timedOutTransactions > 0) {
        line.textContent += ` Timeouts=${data.timedOutTransactions}`;
    }

    if (data.rehydrationRttMs !== undefined && data.rehydrationRttMs > 0) {
        line.textContent += ` RTT=${data.rehydrationRttMs}ms`;
    }
    else { line.textContent += ` RTT=${data.rehydrationRttMs}ms`; }

    log.appendChild(line);

    while (log.children.length > 20) {
        log.removeChild(log.firstChild);
    }
}

function handleServerEvent(event) {
  const time = new Date(event.timestamp).toLocaleTimeString();

  const line = {
    level: event.level,
    text: `[${time}] [${event.category}] ${event.message}`
  };

  serverLogBuffer.push(line);
  if (serverLogBuffer.length > SERVER_LOG_LIMIT) {
    serverLogBuffer.shift();
  }

  renderServerLog();

  // Escalate critical events
  if (event.level === "ERROR") {
    showGlobalAlert(line.text);
  }
}

function handleRegistryChanged(payload) {
    console.info("[Settings] Registry changed:", payload);
    resetTelemetryDisplay();

    const telemetryLog = document.getElementById("telemetry-log");
    if (telemetryLog) telemetryLog.innerHTML = "";

    serverLogBuffer.length = 0;
    renderServerLog();

    const tableBody = document.getElementById("mapping-table-body");
    if (tableBody) tableBody.innerHTML = "";

    showStatus(`Switched to ${payload.profile}`);
}

function resetTelemetryDisplay() {
    const ids = [
        "telemetry-in",
        "telemetry-out",
        "telemetry-combined",
        "telemetry-inflight",
        "telemetry-dropped",
        "remaining-capacity"
    ];

    for (const id of ids) {
        const el = document.getElementById(id);
        if (el) el.textContent = "–";
    }

    const extra = [
        "telemetry-inflight-requests",
        "telemetry-timeout-requests",
        "telemetry-rehydration-rtt"
    ];

    for (const id of extra) {
        const el = document.getElementById(id);
        if (el) el.textContent = "–";
    }
}


function renderServerLog() {
  serverLog.innerHTML = "";

  for (const entry of serverLogBuffer) {
    const div = document.createElement("div");
    div.className = `server-log-line ${entry.level}`;
    div.textContent = entry.text;
    serverLog.appendChild(div);
  }

  serverLog.scrollTop = serverLog.scrollHeight;
}


function showGlobalAlert(message) {
  const el = document.createElement("div");
  el.className = "global-alert";
  el.textContent = message;

  document.body.appendChild(el);

  setTimeout(() => el.remove(), 6000);
}