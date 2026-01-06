console.log(">>> websocketClient.js LOADED <<<");
const debugFlag = false;
export class WebSocketClient {
  constructor(url) {
    this.url = url;
    this.ws = null;

    this.handlers = {
      "ui-model": [],
      "ui-bank": [],
      "control-update": [],
      "error": [],
      "connected": [],
      "disconnected": [],
      "midi-device-list": []
    };

    this.requestCounter = 0;

    console.log("[WebSocketClient] Initialized with URL:", url);
  }


  // ------------------------------------------------------------
  // Connection
  // ------------------------------------------------------------
  connect() {
    console.log("[WebSocketClient] Connecting to:", this.url);
    this.ws = new WebSocket(this.url);

    this.ws.onopen = () => {
      console.log("[WebSocketClient] Connection opened");
      this._emit("connected");
    };

    this.ws.onclose = () => {
      console.log("[WebSocketClient] Connection closed");
      this._emit("disconnected");
    };

    this.ws.onmessage = (event) => {
      console.log("[WebSocketClient] Message received:", event.data);
      try {
        const msg = JSON.parse(event.data);
        this._routeMessage(msg);
      } catch (e) {
        console.warn("[WebSocketClient] Failed to parse message:", e);
      }
    };

    this.ws.onerror = (err) => {
      console.error("[WebSocketClient] WebSocket error:", err);
    };
  }

  // ------------------------------------------------------------
  // Event subscription
  // ------------------------------------------------------------
  on(type, handler) {
    if (!this.handlers[type]) {
      throw new Error(`Unknown event type: ${type}`);
    }
    this.handlers[type].push(handler);
    console.log(`[WebSocketClient] Handler registered for: ${type}`);
  }

  _emit(type, payload) {
    if (!this.handlers[type]) return;
    for (const h of this.handlers[type]) {
      h(payload);
    }
  }

  // ------------------------------------------------------------
  // Message routing
  // ------------------------------------------------------------
  _routeMessage(msg) {
    console.log("[WebSocketClient] Routing message:", msg);

    switch (msg.type) {

      case "ui-bank":
        this._emit("ui-bank", msg);
        break;

      case "ui-model":
        this._emit("ui-model", msg);
        break;

      case "control-update":
        this._emit("control-update", msg.payload);
        break;

      case "error":
        this._emit("error", msg.payload);
        break;

      case "ack":
        if (debugFlag != true) console.debug("[WebSocketClient] ACK received:", msg.payload);
        break;

      case "midi-device-list":
        this._emit("midi-device-list", msg.payload.devices);
        break;

      default:
        console.warn("[WebSocketClient] Unknown message type:", msg.type);
        this._emit("error", msg);
        break;
    }
  }

  // ------------------------------------------------------------
  // Outgoing messages
  // ------------------------------------------------------------
  requestUiModel(contextId) {
    const requestId = `req-${++this.requestCounter}`;
    const message = {
      type: "get-ui-model",
      requestId,
      payload: { contextId }
    };
    console.log("[WebSocketClient] Sending UI model request:", message);
    this.ws.send(JSON.stringify(message));
    return requestId;
  }

  subscribe(contextId) {
    const message = {
      type: "subscribe-context",
      payload: { contextId }
    };
    console.log("[WebSocketClient] Sending subscribe request:", message);
    this.ws.send(JSON.stringify(message));
  }

  sendControlChange(canonicalId, value) {
    const message = {
      type: "set-control-value",
      payload: { canonicalId, value }
    };
    console.log("[WebSocketClient] Sending control change:", message);
    this.ws.send(JSON.stringify(message));
  }

  requestMidiDevices() {
    const message = {
      type: "list-midi-devices",
      requestId: `req-${++this.requestCounter}`,
      payload: {}
    };
    console.log("[WebSocketClient] Requesting MIDI devices:", message);
    this.ws.send(JSON.stringify(message));
  }

  applyMidiSettings(settings) {
    const message = {
      type: "apply-midi-settings",
      requestId: `req-${++this.requestCounter}`,
      payload: settings
    };
    console.log("[WebSocketClient] Applying MIDI settings:", message);
    this.ws.send(JSON.stringify(message));
  }

}