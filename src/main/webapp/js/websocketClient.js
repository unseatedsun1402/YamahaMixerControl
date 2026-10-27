console.log(">>> websocketClient.js LOADED <<<");

const debugFlag = true;
const AUTH_TOKEN_STORAGE_KEY = "yamahaMixer.authToken";
const FINGERPRINT_SALT_STORAGE_KEY = "yamahaMixer.fingerprintSalt";

export class WebSocketClient {
  constructor(url) {
    const socketUrl = new URL(url, window.location.href);
    socketUrl.protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
    this.url = socketUrl.toString();
    this.ws = null;

    this.reconnectDelay = 1000;
    this.maxReconnectDelay = 15000;
    this.reconnectAttempts = 0;
    this.forcedClose = false;

    this.handlers = {
      "ui-model": [],
      "ui-bank": [],
      "control-update": [],
      "meter-update": [],
      "name-update": [],
      "error": [],
      "connected": [],
      "disconnected": [],
      "authentication-result": [],
      "midi-device-list": [],
      "telemetry": [],
      "server-event": [],
      "REGISTRY_CHANGED": []
    };

    this.requestCounter = 0;
    this.sessionType = null;
    this._lastSequence = undefined;
    this.authToken = null;
    this.fingerprintSalt = null;
    this.role = null;
    this.authenticationOptions = null;
    this.authDialog = null;
    this.authStatus = null;

    console.info("[WebSocketClient] Initialized with URL:", url);
  }

  setSessionType(type) {
    this.sessionType = type;
    this._send({
      type: "register-session",
      payload: { sessionType: `${this.sessionType}` }
    });
  }

  connect() {
    console.info("[WebSocketClient] Connecting to:", this.url);
    this.ws = new WebSocket(this.url);

    this.ws.onopen = () => {
      console.info("[WebSocketClient] Connection opened");
      this.authToken = null;
      this.role = null;
      this.fingerprintSalt = this._getOrCreateFingerprintSalt();
      this.ws.send(JSON.stringify({
        type: "get-authentication-options",
        requestId: `auth-options-${++this.requestCounter}`
      }));
      const savedToken = this._getSavedAuthToken();
      if (savedToken) {
        this._authenticateWithToken(savedToken);
      } else {
        this._showAuthenticationDialog();
      }

      this.reconnectDelay = 1000;
      this.reconnectAttempts = 0;
    };

    this.ws.onclose = () => {
      this.authToken = null;
      this.role = null;
      if (this.authDialog?.open) {
        this.authDialog.close();
      }
      const passwordInput = this.authDialog?.querySelector('input[name="password"]');
      if (passwordInput) {
        passwordInput.value = "";
      }
      this._emit("disconnected");
      console.warn("[WebSocketClient] Connection closed");

      if (!this.forcedClose) {
        this._scheduleReconnect();
      }
    };

    this.ws.onmessage = (event) => {
      if (debugFlag) {
        console.debug("[WebSocketClient] Message received:", event.data);
      }

      try {
        const msg = JSON.parse(event.data);
        this._routeMessage(msg);
      } catch (e) {
        console.warn("[WebSocketClient] Failed to parse message:", {
          error: e,
          raw: event.data
        });
      }
    };

    this.ws.onerror = (err) => {
      console.error("[WebSocketClient] WebSocket error:", err);
    };
  }

  // ------------------------------------------------------------

  on(type, handler) {
    if (!this.handlers[type]) {
      throw new Error(`Unknown event type: ${type}`);
    }
    this.handlers[type].push(handler);
    console.info(`[WebSocketClient] Handler registered for: ${type}`);
  }

  _emit(type, payload) {
    if (!this.handlers[type]) return;
    for (const h of this.handlers[type]) {
      h(payload);
    }
  }

  // ------------------------------------------------------------

  _shouldHandle(type) {
    if (!this.sessionType) return true;

    if (this.sessionType === "settings") {
      if (type === "meter-update" || type === "meter-batch") return false;
    }

    if (this.sessionType === "control") {
      if (type === "telemetry") return false;
    }

    if (this.sessionType === "mix") {
      if (type === "telemetry") return false;
    }

    return true;
  }

  _routeMessage(msg) {
    const {
      classification = "UNKNOWN",
      type,
      sequence,
      timestamp,
      requestId
    } = msg;

    const normalizedClassification = classification.toUpperCase();

    if (!type) {
      console.error("[WebSocketClient] Missing type", msg);
      return;
    }

    if (type === "authentication-result") {
      this._handleAuthenticationResult(msg.payload || {});
      this._emit("authentication-result", msg.payload || {});
      return;
    }

    if (type === "authentication-options") {
      this._handleAuthenticationOptions(msg.payload || {});
      return;
    }

    const enrichedPayload = {
      ...(msg.payload || {}),
      __meta: {
        classification: normalizedClassification,
        type,
        sequence,
        timestamp,
        requestId
      }
    };

    // classification-based logging
    if (normalizedClassification === "ERROR") {
      console.error("[WebSocketClient] Server ERROR", {
        type,
        sequence,
        payload: msg.payload
      });
    }

    // sequence tracking
    if (sequence !== undefined) {

        if (this._lastSequence !== undefined &&
            sequence !== this._lastSequence + 1) {

            console.warn("[WebSocketClient] Sequence out of order", {
                expected: this._lastSequence + 1,
                received: sequence,
                type,
                classification
            });
        }
        this._lastSequence = sequence;
    }

    if (!this._shouldHandle(type)) {
      if (debugFlag) {
        console.debug("[WebSocketClient] Dropped by context", {
          sessionType: this.sessionType,
          type
        });
      }
      return;
    }

    switch (type) {

      case "ui-bank":
        this._emit("ui-bank", enrichedPayload);
        break;

      case "ui-model":
        this._emit("ui-model", enrichedPayload);
        break;

      case "control-update":
        this._emit("control-update", enrichedPayload);
        break;

      case "meter-update":
        this._emit("meter-update", enrichedPayload);
        break;

      case "meter-batch": {
        const updates = msg.payload?.updates;
        if (!Array.isArray(updates)) {
          console.error("[WebSocketClient] Invalid meter batch payload", {
            payload: msg.payload,
            sequence
          });
          break;
        }

        for (const update of updates) {
          this._emit("meter-update", {
            ...update,
            __meta: enrichedPayload.__meta
          });
        }
        break;
      }

      case "error":
        if (msg.payload?.code === "UNAUTHENTICATED") {
          this._clearSavedAuthToken();
          this.authToken = null;
          this.role = null;
          this._showAuthenticationDialog();
          if (this.authStatus) {
            this.authStatus.textContent = "Your session is no longer authenticated. Please sign in again.";
          }
        }
        this._emit("error", enrichedPayload);
        break;

      case "ack":
        if (debugFlag) {
          console.debug("[WebSocketClient] ACK:", {
            sequence,
            requestId,
            payload: msg.payload
          });
        }
        break;

      case "midi-device-list": {
        const devices = msg.payload?.devices || [];

        if (!Array.isArray(devices)) {
          console.error("[WebSocketClient] Invalid device list payload", {
            received: msg.payload,
            type,
            sequence
          });
        }

        devices.__meta = enrichedPayload.__meta;

        this._emit("midi-device-list", devices);
        break;
      }

      case "name-update":
        this._emit("name-update", enrichedPayload);
        break;

      case "telemetry":
        this._emit("telemetry", enrichedPayload);
        break;

      case "server-event": {
        this._emit("server-event", enrichedPayload);
        break;
      }

      case "REGISTRY_CHANGED":
        this._emit("REGISTRY_CHANGED", enrichedPayload);
        break;

      default: {
        const level =
          normalizedClassification === "ERROR" ? "error" :
          normalizedClassification === "RESPONSE" ? "error" :
          "warn";

        console[level]("[WebSocketClient] Unhandled message", {
          type,
          classification,
          sequence,
          payload: msg.payload
        });

        break;
      }
    }
  }

  // ------------------------------------------------------------

  _scheduleReconnect() {
    this.reconnectAttempts++;

    console.info(
      `[WebSocketClient] Attempting reconnect #${this.reconnectAttempts} in ${this.reconnectDelay}ms`
    );

    setTimeout(() => {
      this.connect();

      this.reconnectDelay = Math.min(
        this.reconnectDelay * 1.5,
        this.maxReconnectDelay
      );
    }, this.reconnectDelay);
  }

  _showAuthenticationDialog() {
    if (!this.authDialog) {
      const dialog = document.createElement("dialog");
      dialog.setAttribute("aria-labelledby", "mixer-auth-title");
      dialog.style.cssText = [
        "border: 0",
        "border-radius: 10px",
        "box-shadow: 0 12px 40px #0008",
        "max-width: 360px",
        "width: calc(100% - 48px)",
        "padding: 24px",
        "font: inherit"
      ].join(";");
      dialog.innerHTML = `
        <form method="dialog" style="display:grid;gap:12px">
          <h2 id="mixer-auth-title" style="margin:0">Connect to mixer</h2>
          <label>
            Role
            <select name="role" required style="display:block;width:100%;padding:8px">
              <option value="guest">Guest (view only)</option>
              <option value="operator" disabled>Operator (checking availability…)</option>
              <option value="admin" disabled>Admin (checking availability…)</option>
            </select>
          </label>
          <label data-password-label>
            Password
            <input name="password" type="password" autocomplete="current-password"
                   minlength="4"
                   style="display:block;width:100%;padding:8px">
          </label>
          <p data-auth-status role="alert" aria-live="polite" style="margin:0;color:#a00"></p>
          <button type="submit" style="padding:10px">Connect</button>
        </form>`;

      const form = dialog.querySelector("form");
      const roleSelect = form.elements.namedItem("role");
      this.operatorRoleOption = roleSelect.querySelector('option[value="operator"]');
      this.adminRoleOption = roleSelect.querySelector('option[value="admin"]');
      const passwordInput = form.elements.namedItem("password");
      const passwordLabel = dialog.querySelector("[data-password-label]");
      const submitButton = form.querySelector("button");
      this.authStatus = dialog.querySelector("[data-auth-status]");

      const updatePasswordVisibility = () => {
        const needsPassword = roleSelect.value !== "guest";
        passwordLabel.hidden = !needsPassword;
        passwordInput.required = needsPassword;
        if (!needsPassword) {
          passwordInput.value = "";
        }
      };
      roleSelect.addEventListener("change", updatePasswordVisibility);
      updatePasswordVisibility();
      this._updateAuthenticationRoleOptions();

      dialog.addEventListener("cancel", (event) => event.preventDefault());
      form.addEventListener("submit", (event) => {
        event.preventDefault();
        if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
          this.authStatus.textContent = "The server connection is unavailable. Reconnecting…";
          return;
        }

        submitButton.disabled = true;
        this.authStatus.textContent = "Authenticating…";
        this.ws.send(JSON.stringify({
          type: "authenticate",
          requestId: `auth-${++this.requestCounter}`,
          payload: {
            role: roleSelect.value,
            fingerprintSalt: this.fingerprintSalt
          }
        }));
      });

      document.body.appendChild(dialog);
      this.authDialog = dialog;
    }

    this.authStatus.textContent = "";
    const submitButton = this.authDialog.querySelector("button");
    submitButton.disabled = false;
    if (!this.authDialog.open) {
      this.authDialog.showModal();
    }
  }

  _getSavedAuthToken() {
    try {
      return window.localStorage.getItem(AUTH_TOKEN_STORAGE_KEY);
    } catch (error) {
      console.error("[WebSocketClient] Could not read the saved authentication token:", error);
      return null;
    }
  }

  _handleAuthenticationOptions(options) {
    this.authenticationOptions = {
      guest: options.guest === true,
      operator: options.operator === true,
      admin: options.admin === true
    };
    this._updateAuthenticationRoleOptions();
  }

  _updateAuthenticationRoleOptions() {
    if (!this.operatorRoleOption || !this.adminRoleOption) {
      return;
    }

    for (const [option, role, label] of [
      [this.operatorRoleOption, "operator", "Operator"],
      [this.adminRoleOption, "admin", "Admin"]
    ]) {
      const enabled = this.authenticationOptions?.[role] === true;
      option.disabled = !enabled;
      option.textContent = enabled ? label : `${label} (not configured on server)`;
    }

    const roleSelect = this.authDialog?.querySelector('select[name="role"]');
    if (roleSelect?.selectedOptions[0]?.disabled) {
      roleSelect.value = "guest";
      roleSelect.dispatchEvent(new Event("change"));
    }
  }

  _getOrCreateFingerprintSalt() {
    try {
      let salt = window.localStorage.getItem(FINGERPRINT_SALT_STORAGE_KEY);
      if (!salt) {
        const bytes = new Uint8Array(32);
        window.crypto.getRandomValues(bytes);
        salt = Array.from(bytes, (byte) => byte.toString(16).padStart(2, "0")).join("");
        window.localStorage.setItem(FINGERPRINT_SALT_STORAGE_KEY, salt);
      }
      return salt;
    } catch (error) {
      console.error("[WebSocketClient] Could not access the fingerprint salt:", error);
      const bytes = new Uint8Array(32);
      window.crypto.getRandomValues(bytes);
      return Array.from(bytes, (byte) => byte.toString(16).padStart(2, "0")).join("");
    }
  }

  _storeAuthToken(token) {
    try {
      window.localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, token);
      if (this.fingerprintSalt) {
        window.localStorage.setItem(FINGERPRINT_SALT_STORAGE_KEY, this.fingerprintSalt);
      }
    } catch (error) {
      console.error("[WebSocketClient] Could not save the authentication token:", error);
    }
  }

  _clearSavedAuthToken() {
    try {
      window.localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY);
      window.localStorage.removeItem(FINGERPRINT_SALT_STORAGE_KEY);
    } catch (error) {
      console.error("[WebSocketClient] Could not clear the saved authentication token:", error);
    }
  }

  _authenticateWithToken(token) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.warn("[WebSocketClient] Could not reauthenticate: WebSocket is not open.");
      this._showAuthenticationDialog();
      return;
    }

    this._resumeAttempt = true;
    this.ws.send(JSON.stringify({
      type: "authenticate",
      requestId: `auth-${++this.requestCounter}`,
      payload: { token, fingerprintSalt: this.fingerprintSalt }
    }));
  }

  _handleAuthenticationResult(payload) {
    if (payload.status === "challenge") {
      this._answerPasswordChallenge(payload);
      return;
    }

    const form = this.authDialog?.querySelector("form");
    const submitButton = form?.querySelector("button");
    if (submitButton) {
      submitButton.disabled = false;
    }

    if (payload.status !== "ok" || typeof payload.token !== "string" || !payload.token) {
      const wasResumeAttempt = this._resumeAttempt;
      this._resumeAttempt = false;
      if (wasResumeAttempt) {
        this._clearSavedAuthToken();
        this.authToken = null;
        this.role = null;
        this._showAuthenticationDialog();
        if (this.authStatus) {
          this.authStatus.textContent = payload.message || "Saved login expired. Please sign in again.";
        }
        return;
      }

      if (this.authStatus) {
        this.authStatus.textContent = payload.message || "Authentication failed.";
      }
      const passwordInput = form?.elements.namedItem("password");
      if (passwordInput) {
        passwordInput.value = "";
      }
      return;
    }

    this._resumeAttempt = false;
    this.authToken = payload.token;
    this.role = payload.role;
    this._storeAuthToken(payload.token);
    const passwordInput = form?.elements.namedItem("password");
    if (passwordInput) {
      passwordInput.value = "";
    }
    if (this.authDialog?.open) {
      this.authDialog.close();
    }
    this._emit("connected", { role: this.role });
  }

  async _answerPasswordChallenge(challenge) {
    const form = this.authDialog?.querySelector("form");
    const passwordInput = form?.elements.namedItem("password");
    const submitButton = form?.querySelector("button");
    try {
      if (!passwordInput?.value || !this.fingerprintSalt) {
        throw new Error("Enter a password to continue.");
      }
      if (!window.crypto?.subtle) {
        throw new Error("Secure password authentication requires HTTPS or localhost.");
      }

      const key = await window.crypto.subtle.importKey(
        "raw",
        await window.crypto.subtle.deriveBits(
          {
            name: "PBKDF2",
            salt: this._decodeBase64Url(challenge.passwordSalt),
            iterations: challenge.iterations,
            hash: "SHA-256"
          },
          await window.crypto.subtle.importKey(
            "raw",
            new TextEncoder().encode(passwordInput.value),
            "PBKDF2",
            false,
            ["deriveBits"]
          ),
          256
        ),
        { name: "HMAC", hash: "SHA-256" },
        false,
        ["sign"]
      );
      passwordInput.value = "";
      const challengeMessage = [
        challenge.challengeId,
        challenge.role,
        this.fingerprintSalt
      ].join("\n");
      const signature = await window.crypto.subtle.sign(
        "HMAC",
        key,
        new TextEncoder().encode(challengeMessage)
      );
      if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
        throw new Error("The server connection is unavailable. Reconnecting…");
      }

      const proofBytes = new Uint8Array(signature);
      let proofBinary = "";
      for (const byte of proofBytes) {
        proofBinary += String.fromCharCode(byte);
      }
      const proof = window.btoa(proofBinary)
        .replace(/\+/g, "-")
        .replace(/\//g, "_")
        .replace(/=+$/, "");

      this.authStatus.textContent = "Verifying password…";
      this.ws.send(JSON.stringify({
        type: "authenticate",
        requestId: `auth-${++this.requestCounter}`,
        payload: {
          challengeId: challenge.challengeId,
          proof,
          fingerprintSalt: this.fingerprintSalt
        }
      }));
    } catch (error) {
      if (passwordInput) {
        passwordInput.value = "";
      }
      if (submitButton) {
        submitButton.disabled = false;
      }
      this.authStatus.textContent = error instanceof Error
        ? error.message
        : "Could not complete password authentication.";
    }
  }

  _decodeBase64Url(value) {
    const base64 = value.replace(/-/g, "+").replace(/_/g, "/");
    const paddedBase64 = base64.padEnd(Math.ceil(base64.length / 4) * 4, "=");
    const binary = window.atob(paddedBase64);
    return Uint8Array.from(binary, (character) => character.charCodeAt(0));
  }

  _send(message) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.warn("[WebSocketClient] Request not sent: WebSocket is not open.");
      return false;
    }
    if (!this.authToken) {
      console.warn("[WebSocketClient] Request not sent: authenticate first.");
      return false;
    }
    this.ws.send(JSON.stringify({
      ...message,
      payload: {
        ...(message.payload || {}),
        authToken: this.authToken,
        fingerprintSalt: this.fingerprintSalt
      }
    }));
    return true;
  }

  // ------------------------------------------------------------

  requestUiModel(contextId, uiType = "basic-input-view") {
    const requestId = `req-${++this.requestCounter}`;
    const message = {
      type: "get-ui-model",
      requestId,
      payload: { contextId, uiType }
    };

    console.debug("[WebSocketClient] Requesting UI model:", message);
    this._send(message);
    return requestId;
  }

  requestBank(bankId) {
    const message = {
      type: "get-ui-bank",
      payload: { bankId }
    };

    console.debug("[WebSocketClient] Requesting UI bank:", message);
    this._send(message);
  }

  subscribe(contextId) {
    const message = {
      type: "subscribe-context",
      payload: { contextId }
    };

    console.debug("[WebSocketClient] Sending subscribe request:", message);
    this._send(message);
  }

  sendControlChange(canonicalId, value) {
    const message = {
      type: "set-control-value",
      payload: { canonicalId, value }
    };

    if (debugFlag) {
      console.debug("[WebSocketClient] Sending control change:", message);
    }

    this._send(message);
  }

  sendMeterKeepAlive() {
    const message = {
      type: "meter-keep-alive",
      payload: { timestamp: Date.now() }
    };

    if (debugFlag) {
      console.debug("[WebSocketClient] Sending meter keep alive");
    }

    this._send(message);
  }

  requestMidiDevices() {
    const message = {
      type: "list-midi-devices",
      requestId: `req-${++this.requestCounter}`,
      payload: {}
    };

    console.info("[WebSocketClient] Requesting MIDI devices:", message);
    this._send(message);
  }

  applyMidiSettings(settings) {
    const message = {
      type: "apply-midi-settings",
      requestId: `req-${++this.requestCounter}`,
      payload: settings
    };

    console.info("[WebSocketClient] Applying MIDI settings:", message);
    this._send(message);
  }

  saveMidiSettings(settings) {
    const message = {
      type: "save-midi-settings",
      requestId: `req-${++this.requestCounter}`,
      payload: settings
    };

    console.info("[WebSocketClient] Saving MIDI settings:", message);
    this._send(message);
  }

  requestChannelNames() {
    const message = {
      type: "request-channel-names",
      requestId: `req-${++this.requestCounter}`,
      payload: { empty: true }
    };

    this._send(message);
    console.info("[WebSocketClient] Requesting Channel Names", message);
  }
  
  setDebugResolution(enabled) {
      const message = {
      "type": "set-debug-flag",
      "payload": {
        "flag": "resolution",
        "enabled": enabled
        }
      }

      console.info(
          "[WebSocketClient] Setting Debug Resolution:",
          enabled
      );

      this._send(message);
  }

  setCanonicalEvents(enabled) {
      const message = {
      "type": "set-debug-flag",
      "payload": {
        "flag": "canonical",
        "enabled": enabled
        }
      }


      console.info(
          "[WebSocketClient] Setting Canonical Events:",
          enabled
      );

      this._send(message);
  }
}
