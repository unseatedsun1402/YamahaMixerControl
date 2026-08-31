export class DeskStatusWidget {
    constructor(element) {
    this.el = element;
    loadLivenessCss();
    this.el.textContent = "Waiting for desk status…";
    this.el.className = "desk-dead";
    this.el.lastState = "desk-dead";
    }

  update(payload) {
    const { message, details } = payload;

    const alive = details?.isConnected === true;

    this.el.textContent = message;
    this.el.className = alive ? "desk-alive" : "desk-dead";
    showAlert(this.el);
    this.el.lastState = alive ? "desk-alive" : "desk-dead";
  }
}

function loadLivenessCss() {
  const link = document.createElement("link");
  link.rel = "stylesheet";
  link.href = "css/widgets/liveness.css";
  document.head.appendChild(link);
}

function showAlert(el){
    if (el.lastState === "desk-alive" && el.className === "desk-dead"){
        alert(message)
    }
    if (el.lastState === "desk-dead" && el.className === "desk-alive"){
        alert(message)
    }
}
