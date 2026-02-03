console.log(">>> websocketClient.js LOADED <<<");

const debugFlag = false;

export class WebSocketClient {
  constructor(url) {
    this.url = url;
    this.ws = null;

    
    this.reconnectDelay = 1000;      // start at 1 second
    this.maxReconnectDelay = 15000;  // cap at 15 seconds
    this.reconnectAttempts = 0;
    this.forcedClose = false;        // manual close flag

    this.handlers = {
      "ui-model": [],
      "ui-bank": [],
      "control-update": [],
      "meter-update": [],
      "name-update": [],
      "error": [],
      "connected": [],
      "disconnected": [],
      "midi-device-list": []
    };

    this.requestCounter = 0;

    console.info("[WebSocketClient] Initialized with URL:", url);
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
      if (debugFlag) {console.debug("[WebSocketClient] Message received:", event.data);}
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

  _routeMessage(msg) {
    // console.debug("[WebSocketClient] Routing message:", msg);

    switch (msg.type) {

      case "ui-bank":
        this._emit("ui-bank", msg.payload);
        break;

      case "ui-model":
        this._emit("ui-model", msg.payload);
        break;

      case "control-update":
        this._emit("control-update", msg.payload);
        break;
      
      case "meter-update":
        this._emit("meter-update", msg.payload);
        break;

      case "error":
        this._emit("error", msg.payload);
        break;

      case "ack":
        if (!debugFlag) console.debug("[WebSocketClient] ACK:", msg.payload);
        break;

      case "midi-device-list":
        this._emit("midi-device-list", msg.payload.devices);
        break;
      
      case "name-update":
        this._emit("name-update", msg.payload);
        break;

      default:
        console.warn("[WebSocketClient] Unknown message type:", msg.JSON);
        this._emit("error", msg);
        break;
    }
  }

  
  _scheduleReconnect() {
    this.reconnectAttempts++;
    console.info(
      `[WebSocketClient] Attempting reconnect #${this.reconnectAttempts} in ${this.reconnectDelay}ms`
    );

    setTimeout(() => {
      this.connect();

      // Exponential backoff
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

    console.debug(`[WebSocketClient] Requesting UI model: context=${contextId}, uiType=${uiType}`);
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
    console.info("[WebSocketClient] Sending subscribe request:", message);
    this.ws.send(JSON.stringify(message));
  }

  sendControlChange(canonicalId, value) {
    const message = {
      type: "set-control-value",
      payload: { canonicalId, value }
    };
    if (debugFlag){console.debug("[WebSocketClient] Sending control change: ", message);}
    this.ws.send(JSON.stringify(message));
  }

  sendMeterKeepAlive() {
    const message = {
      type: "meter-keep-alive",
      payload: { "timestamp": Date.now() }
    };
    if (debugFlag){console.debug("[WebSocketClient] Sending meter keep alive: ");}
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

  requestChannelNames(){
    const message = {
      type: "request-channel-names",
      requestId: `req-${++this.requestCounter}`,
      payload: "{ \"empty\" : \"true\" }"
    };
    const json = JSON.stringify(message)
    this.ws.send(json)
    console.info("[WebSocketClient] Requesting Channel Names "+json)
  }
}