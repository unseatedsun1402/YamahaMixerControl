console.log(">>> websocketClient.js LOADED <<<");

const debugFlag = true;
const QUEUE_BEFORE_AUTHENTICATION = new Set([
  "register-session",
  "get-ui-model",
  "get-ui-bank",
  "subscribe-context",
  "list-midi-devices",
  "meter-keep-alive",
  "request-channel-names"
]);

export class WebSocketClient {
  constructor(url) {
    this.url = url;
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
      "midi-device-list": [],
      "telemetry": [],
      "server-event": [],
      "REGISTRY_CHANGED": [],
      "auth-challenge": [],
      "auth-result": [],
      "auth-status": []
    };

    this.requestCounter = 0;
    this.sessionType = null;
    this._lastSequence = undefined;
    this.authenticated = false;
    this._pendingMessages = [];

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
    const url = new URL(this.url);
    url.searchParams.set("connectionId", this._createConnectionId());
    this.ws = new WebSocket(url);

    this.ws.onopen = () => {
      console.info("[WebSocketClient] Connection opened");
      this.authenticated = false;
      this._pendingMessages = [];
      this._emit("connected");

      this.reconnectDelay = 1000;
      this.reconnectAttempts = 0;
    };

    this.ws.onclose = () => {
      this.authenticated = false;
      this._pendingMessages = [];
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

  _createConnectionId() {
    if (globalThis.crypto?.randomUUID) {
      return globalThis.crypto.randomUUID();
    }
    return `${Date.now()}-${Math.random().toString(36).slice(2)}`;
  }

  _send(message) {
    if (this.authenticated && this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
      return true;
    }
    if (!QUEUE_BEFORE_AUTHENTICATION.has(message.type)) {
      console.warn("[WebSocketClient] Dropping request until authenticated:", message.type);
      return false;
    }
    this._pendingMessages.push(message);
    return true;
  }

  _setAuthenticated(authenticated) {
    this.authenticated = authenticated;
    if (!authenticated || this.ws?.readyState !== WebSocket.OPEN) return;
    const pending = this._pendingMessages;
    this._pendingMessages = [];
    for (const message of pending) {
      this.ws.send(JSON.stringify(message));
    }
  }

  sendAuthenticationToken(token) {
    if (this.ws?.readyState !== WebSocket.OPEN) {
      throw new Error("WebSocket is not connected");
    }
    this.ws.send(JSON.stringify({
      type: "authenticate",
      payload: { token }
    }));
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
        this._emit("error", enrichedPayload);
        break;

      case "auth-challenge":
        this._emit("auth-challenge", msg.payload || {});
        break;

      case "auth-result":
        if (msg.payload?.authenticated) this._setAuthenticated(true);
        this._emit("auth-result", msg.payload || {});
        break;

      case "auth-status":
        if (msg.payload?.authenticated) this._setAuthenticated(true);
        this._emit("auth-status", msg.payload || {});
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

  requestMeters(category, source, startChannel, channelCount) {
    const message = {
      type: "request-meters",
      requestId: `req-${++this.requestCounter}`,
      payload: { category, source, startChannel, channelCount }
    };

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
