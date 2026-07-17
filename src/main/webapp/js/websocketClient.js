console.log(">>> websocketClient.js LOADED <<<");

const debugFlag = false;

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
      "REGISTRY_CHANGED": []
    };

    this.requestCounter = 0;
    this.sessionType = null;
    this._lastSequence = undefined;

    console.info("[WebSocketClient] Initialized with URL:", url);
  }

  setSessionType(type) {
    this.sessionType = type;
        this.ws.send(JSON.stringify({
        type: "register-session",
        payload: { sessionType: `${this.sessionType}` }
    }));
  }

  connect() {
    console.info("[WebSocketClient] Connecting to:", this.url);
    this.ws = new WebSocket(this.url);

    this.ws.onopen = () => {
      console.info("[WebSocketClient] Connection opened");
      this._emit("connected");

      this.reconnectDelay = 1000;
      this.reconnectAttempts = 0;
    };

    this.ws.onclose = () => {
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
      if (type === "control-update" || type === "meter-update") return false;
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

    // sequence tracking
    if (
      this._lastSequence !== undefined &&
      sequence !== undefined
    ) {
      if (sequence !== this._lastSequence + 1) {
        console.warn("[WebSocketClient] Sequence out of order", {
          expected: this._lastSequence + 1,
          received: sequence,
          type,
          classification
        });
      }
    }

    this._lastSequence = sequence;

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

      case "error":
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

  // ------------------------------------------------------------

  requestUiModel(contextId, uiType = "basic-input-view") {
    const requestId = `req-${++this.requestCounter}`;
    const message = {
      type: "get-ui-model",
      requestId,
      payload: { contextId, uiType }
    };

    console.debug("[WebSocketClient] Requesting UI model:", message);
    this.ws.send(JSON.stringify(message));
    return requestId;
  }

  requestBank(bankId) {
    const message = {
      type: "get-ui-bank",
      payload: { bankId }
    };

    console.debug("[WebSocketClient] Requesting UI bank:", message);
    this.ws.send(JSON.stringify(message));
  }

  subscribe(contextId) {
    const message = {
      type: "subscribe-context",
      payload: { contextId }
    };

    console.debug("[WebSocketClient] Sending subscribe request:", message);
    this.ws.send(JSON.stringify(message));
  }

  sendControlChange(canonicalId, value) {
    const message = {
      type: "set-control-value",
      payload: { canonicalId, value }
    };

    if (debugFlag) {
      console.debug("[WebSocketClient] Sending control change:", message);
    }

    this.ws.send(JSON.stringify(message));
  }

  sendMeterKeepAlive() {
    const message = {
      type: "meter-keep-alive",
      payload: { timestamp: Date.now() }
    };

    if (debugFlag) {
      console.debug("[WebSocketClient] Sending meter keep alive");
    }

    this.ws.send(JSON.stringify(message));
  }

  requestMidiDevices() {
    const message = {
      type: "list-midi-devices",
      requestId: `req-${++this.requestCounter}`,
      payload: {}
    };

    console.info("[WebSocketClient] Requesting MIDI devices:", message);
    this.ws.send(JSON.stringify(message));
  }

  applyMidiSettings(settings) {
    const message = {
      type: "apply-midi-settings",
      requestId: `req-${++this.requestCounter}`,
      payload: settings
    };

    console.info("[WebSocketClient] Applying MIDI settings:", message);
    this.ws.send(JSON.stringify(message));
  }

  saveMidiSettings(settings) {
    const message = {
      type: "save-midi-settings",
      requestId: `req-${++this.requestCounter}`,
      payload: settings
    };

    console.info("[WebSocketClient] Saving MIDI settings:", message);
    this.ws.send(JSON.stringify(message));
  }

  requestChannelNames() {
    const message = {
      type: "request-channel-names",
      requestId: `req-${++this.requestCounter}`,
      payload: { empty: true }
    };

    const json = JSON.stringify(message);
    this.ws.send(json);
    console.info("[WebSocketClient] Requesting Channel Names", json);
  }
}