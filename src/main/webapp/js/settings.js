import { WebSocketClient } from "./websocketClient.js";
import { DeskStatusWidget } from "./widgets/liveness.js";
import { renderHiddenPatchControls } from "./widgets/patch-matrix.js";
import { renderPatchMatrixOverlay } from "./widgets/patch-matrix.js";
import { applyControlUpdate } from './partial-update.js';
import { buildPatchRibbon } from "./view-ribbon.js";
import { installAuthentication } from "./authentication.js";

console.log(">>> settings.js LOADED <<<");

const ws = new WebSocketClient(`ws://${location.host}/MidiControl/endpoint`);
installAuthentication(ws);
const SERVER_LOG_LIMIT = 50;
const serverLogBuffer = [];
window.addEventListener("beforeunload", saveLogsToLocalStorage);

const serverLog = document.getElementById("server-log");
const deskStatusWidget = new DeskStatusWidget(document.getElementById("desk-status"));

const patchMatrix = document.getElementById("patch-matrix");

window.patchBanks = [
    { id: "bank.inputs", label: "INPUT" },
    { id: "bank.outputs", label: "OUTPUT" },
    { id: "bank.slot", label: "SLOT" },
    { id: "bank.omni", label: "OMNI" },
];


document.querySelector('[data-tab="patching"]').addEventListener("click", () => {
    console.info("[Settings] Entering patching mode");

    window.currentDomain = "patching";
    window.currentView = "patch-view";
    window.currentBank = 0;
    window.ribbonInitialised = false;

    buildPatchRibbon(selectPatchBank);
    window.ribbonInitialised = true;

    ws.requestBank("bank.outputs");
});

function dump(obj) {
    return JSON.stringify(obj, null, 2);
}

ws.connect();
window.wsClient = ws;

ws.on("connected", () => {
  console.log("[Settings] Connected, requesting device list...");
  ws.requestMidiDevices();
  loadServerInterfaceOptions();
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

function selectPatchBank(bankId) {
    window.currentDomain = "patching";
    window.currentView = "patch-view";

    window.currentBank = 0;
    window.ribbonInitialised = false;

    window.currentBankId = bankId;
    ws.requestBank(bankId);
}

let expectedChannels = 0;
ws.on("ui-bank", (bank) => {
    const container = document.getElementById("patch-matrix");

    container.innerHTML = "";
    container._hiddenControls = new Map();

    expectedChannels = bank.contexts.length;

    console.info("Received ui bank - requesting uiModels: "+dump(bank));

    for (const ctxId of bank.contexts) {
        ws.subscribe(ctxId);
        ws.requestUiModel(ctxId, "basic-patch-view");
    }
});

ws.on("ui-model", async model => {
    if (model.viewType !== "basic-patch-view") {
        console.warn(`${model.viewType} is an unknown type and not handled`);
        return;
    }

    const container = document.getElementById("patch-matrix");

    renderHiddenPatchControls(model, container);

    if (container._hiddenControls.size === expectedChannels) {

        if (!container._channelMapType ||
            container._channelMapType !== getMapEndpoint(window.currentBankId)) {

            const contextPath = window.location.pathname.split('/')[1];
            const mapType = getMapEndpoint(window.currentBankId);

            const map = await fetch(
                `http://${location.host}/${contextPath}/${mapType}`
            ).then(r => r.json());

            if (!Array.isArray(map)) {
                console.warn(`${mapType} not ready yet:`, map);
                return;
            }

            container._channelMap = map;
            container._channelMapType = mapType;
        }

        renderPatchMatrixOverlay(container);
    }
});

ws.on("control-update", (payload) => {
    if (payload.canonicalId.includes("kChannelIn")) {
        console.info("[Bootstrap] Control update:", payload);
    }
    applyControlUpdate(payload);
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
  if (!ws.authenticated) {
    showStatus("Authenticate before applying settings");
    return;
  }
  const inputDeviceId = parseInt(document.getElementById("midi-input-device").value, 10);
  const outputDeviceId = parseInt(document.getElementById("midi-output-device").value, 10);
  const consoleType = document.getElementById("console-type").value;

  const settings = {
    inputDeviceId,
    outputDeviceId,
    consoleType,
    safeprofile: document.getElementById("safe-profile").checked,
    mainprofile: document.getElementById("main-profile").checked,
    highprofile: document.getElementById("high-profile").checked
  };

  ws.applyMidiSettings(settings);
  showStatus("Settings applied");
});

document.getElementById("save-settings").addEventListener("click", () => {
  if (!ws.authenticated) {
    showStatus("Authenticate before saving settings");
    return;
  }
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

async function loadServerInterfaceOptions() {
  const select = document.getElementById("server-interface-select");
  const applyButton = document.getElementById("apply-network-choice");
  if (!select) return;

  const url = new URL("get-server-ip", window.location.href);
  url.searchParams.set("action", "list");

  try {
    const response = await fetch(url, { cache: "no-store" });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);

    const result = await response.json();
    const options = Array.isArray(result?.payload) ? result.payload : [];

    select.innerHTML = "";

    if (!options.length) {
      const none = document.createElement("option");
      none.value = "";
      none.textContent = "No LAN interfaces found";
      select.appendChild(none);
      if (applyButton) applyButton.disabled = true;
      return;
    }

    options.forEach((optionData) => {
      const option = document.createElement("option");
      option.value = optionData.address || "";
      option.textContent = optionData.preferred
        ? `${optionData.address} (preferred)`
        : optionData.address;
      if (optionData.preferred) option.selected = true;
      select.appendChild(option);
    });

    if (applyButton) applyButton.disabled = false;
  }
  catch (error) {
    console.warn("[Settings] Failed to load interface list:", error);
    select.innerHTML = "";
    const fallback = document.createElement("option");
    fallback.value = "";
    fallback.textContent = "Unable to detect interfaces";
    select.appendChild(fallback);
    if (applyButton) applyButton.disabled = true;
  }
}

async function applyServerInterfaceSelection() {
  const select = document.getElementById("server-interface-select");
  if (!select || !select.value) {
    showStatus("No interface selected");
    return;
  }

  const url = new URL("get-server-ip", window.location.href);
  url.searchParams.set("action", "select");
  url.searchParams.set("preferredHost", select.value);

  try {
    const response = await fetch(url, { cache: "no-store" });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);

    const result = await response.json();
    const selected = result?.payload?.selected || select.value;
    showStatus(`Serving on ${selected}`);
    await loadServerInterfaceOptions();
  }
  catch (error) {
    console.warn("[Settings] Failed to update interface selection:", error);
    showStatus("Unable to save interface choice");
  }
}

document.getElementById("apply-network-choice")?.addEventListener("click", applyServerInterfaceSelection);

const passwordDialog = document.getElementById("system-password-dialog");
const passwordForm = document.getElementById("system-password-form");
const passwordInput = document.getElementById("system-password");
const passwordConfirmInput = document.getElementById("system-password-confirm");
const passwordStatus = document.getElementById("system-password-status");
const passwordSubmitButton = document.getElementById("submit-system-password");
const passwordDialogButton = document.getElementById("open-password-dialog");
const passwordHelp = document.getElementById("password-setting-help");
const normalizedHostname = window.location.hostname.replace(/^\[|\]$/g, "").toLowerCase();
const isLocalSession = normalizedHostname === "localhost"
    || normalizedHostname === "127.0.0.1"
    || normalizedHostname === "::1";

passwordDialogButton.disabled = !isLocalSession;
passwordDialogButton.title = isLocalSession
    ? ""
    : "Open settings on the server computer using localhost to set a password";
if (!isLocalSession) {
    passwordHelp.textContent =
        "Unavailable from this device. Open this page using localhost on the server computer to set a password.";
}

passwordDialogButton.addEventListener("click", () => {
    passwordStatus.textContent = "";
    passwordDialog.showModal();
    passwordInput.focus();
});

document.getElementById("cancel-password-dialog").addEventListener("click", () => {
    passwordDialog.close();
});

passwordDialog.addEventListener("close", () => {
    passwordForm.reset();
    passwordStatus.textContent = "";
    passwordSubmitButton.disabled = false;
});

passwordForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (passwordInput.value !== passwordConfirmInput.value) {
        passwordStatus.textContent = "The passwords do not match.";
        passwordConfirmInput.focus();
        return;
    }

    passwordSubmitButton.disabled = true;
    passwordStatus.textContent = "Saving password…";
    try {
        const url = new URL("set-system-password", window.location.href);
        const response = await fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            cache: "no-store",
            body: JSON.stringify({ password: passwordInput.value })
        });
        const result = await response.json();
        if (!response.ok) {
            throw new Error(result.message || `HTTP ${response.status}`);
        }

        passwordDialog.close();
        showStatus("System password set");
    } catch (error) {
        passwordStatus.textContent = error.message || "Unable to set system password.";
        passwordSubmitButton.disabled = false;
    }
});

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

    if (event.category === "LIVENESS") {
        deskStatusWidget.update(event);
    }

    serverLogBuffer.push(line);
    if (serverLogBuffer.length > SERVER_LOG_LIMIT) {
        serverLogBuffer.shift(); // Remove the oldest log
    }

    renderServerLog();

    if (event.level === "ERROR") {
        showGlobalAlert(line.text);
    }
}

function saveLogsToLocalStorage() {
    localStorage.setItem("serverLogs", JSON.stringify(serverLogBuffer));
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
    const log = document.getElementById("server-log");
    log.innerHTML = ""; // Clear the log

    serverLogBuffer.forEach(logEntry => {
        const logLine = document.createElement("div");
        logLine.textContent = logEntry.text;
        log.appendChild(logLine);
    });

    while (log.children.length > SERVER_LOG_LIMIT) {
        log.removeChild(log.children[0]);
    }
}


function showGlobalAlert(message) {
  const el = document.createElement("div");
  el.className = "global-alert";
  el.textContent = message;

  document.body.appendChild(el);

  setTimeout(() => el.remove(), 6000);
}

document.getElementById("debug-resolution")
    ?.addEventListener("change", e => {

        ws.setDebugResolution(e.target.checked);

        showStatus(
            e.target.checked
                ? "Control resolution logging enabled"
                : "Control resolution logging disabled"
        );
    });

document.getElementById("show-canonical")
    ?.addEventListener("change", e => {

        ws.setCanonicalEvents(e.target.checked);

        showStatus(
            e.target.checked
                ? "Canonical event logging enabled"
                : "Canonical event logging disabled"
        );
    });

function getMapEndpoint(bankId) {
    switch (bankId) {
        case "bank.outputs":
        case "bank.slot":
        case "bank.omni":
            return "dest-map";

        case "bank.inputs":
        default:
            return "source-map";
    }
}

window.addEventListener("load", () => {
    const savedLogs = localStorage.getItem("serverLogs");
    if (savedLogs) {
        try {
            serverLogBuffer = JSON.parse(savedLogs);
            renderServerLog();
        } catch (e) {
            console.warn("Failed to load saved logs:", e);
        }
    }
});
