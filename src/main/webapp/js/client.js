// const version = "202510072035";
Client = {};
Client.socket = null;

Client.connect = (function(host) {
    if ('WebSocket' in window) {
        Client.socket = new WebSocket(host);
    } else if ('MozWebSocket' in window) {
        Client.socket = new MozWebSocket(host);
        } else {
        console.error('Error: WebSocket is not supported by this browser.');
        return;
}

Client.socket.onopen = function () 
{
    console.log('Info: WebSocket connection opened.');
    elements = document.getElementsByClassName("chfader");
    buildpage();
};

Client.socket.onclose = function () {
    console.log('Info: WebSocket closed.');
    alert("Connection Lost");

    setTimeout(() => {
        Client.initialize(); // try reconnecting after a short delay
    }, 300);
};

Client.socket.onmessage = function (message) {
  if (IS_DEBUG) {
    console.log("Received:", message.data);
  }

  try {
    const json = JSON.parse(message.data);

    if (json.type === "nrpnUpdate" || json.type === "outputFaderUpdate") {
      if (IS_DEBUG) {
        console.log("Fader Update:", json);
      }
      handleFaderUpdate(json); // unified handler
    } else if (IS_DEBUG) {
      console.log("Parsed (unhandled type):", json);
    }

  } catch (e) {
    console.error("Invalid JSON from server:", e);
  }
};


Client.socket.onerror = function (error) {
console.error("WebSocket error:", error);
alert("WebSocket error occurred");
};
});

Client.initialize = function() {
if (window.location.protocol == 'http:') {
    Client.connect('ws://' + window.location.host + '/MidiControl/endpoint');
} else {
    Client.connect('wss://' + window.location.host + '/MidiControl/endpoint');
}
};

Client.sendMessage = (function(msg) {
var message = msg;
if (message != '') {
    Client.socket.send(message);
}
else {
    console.warn('%cWarning%c: No message to send.', 'color: orange; font-weight: bold;', 'color: inherit;');
}
});
Client.initialize();