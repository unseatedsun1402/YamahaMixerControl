import { buildBearerToken, hashPassword } from "./authToken.js";

const PASSWORD_HASH_KEY = "MidiControl.passwordHash";

function createElement(tag, text) {
  const element = document.createElement(tag);
  if (text) element.textContent = text;
  return element;
}

export function installAuthentication(ws) {
  const dialog = createElement("dialog");
  dialog.className = "authentication-dialog";

  const form = createElement("form");
  form.method = "dialog";
  const heading = createElement("h2", "Authentication required");
  const instructions = createElement("p", "Enter the system password to connect.");
  const status = createElement("p");
  status.setAttribute("role", "status");
  status.setAttribute("aria-live", "polite");

  const passwordLabel = createElement("label", "System password");
  const passwordInput = createElement("input");
  passwordInput.type = "password";
  passwordInput.autocomplete = "current-password";
  passwordInput.required = true;
  passwordLabel.append(passwordInput);

  const rememberLabel = createElement("label");
  const rememberInput = createElement("input");
  rememberInput.type = "checkbox";
  rememberInput.checked = true;
  rememberLabel.append(rememberInput, document.createTextNode(" Remember on this browser"));

  const actions = createElement("div");
  actions.className = "authentication-actions";
  const cancelButton = createElement("button", "Cancel");
  cancelButton.type = "button";
  const submitButton = createElement("button", "Connect");
  submitButton.type = "submit";
  actions.append(cancelButton, submitButton);

  form.append(heading, instructions, passwordLabel, rememberLabel, status, actions);
  dialog.append(form);
  document.body.append(dialog);

  let challenge = null;
  let pendingPasswordHash = null;
  let attemptedSavedHash = false;

  function showDialog(message = "") {
    status.textContent = message;
    if (!dialog.open) dialog.showModal();
    passwordInput.focus();
  }

  function readSavedHash() {
    try {
      return localStorage.getItem(PASSWORD_HASH_KEY);
    } catch (error) {
      status.textContent = "Browser storage is unavailable; this password will not be remembered.";
      return null;
    }
  }

  function sendProof(passwordHash) {
    const token = buildBearerToken(passwordHash, challenge.salt, challenge.sessionId);
    ws.sendAuthenticationToken(token);
  }

  ws.on("auth-challenge", payload => {
    if (typeof payload.salt !== "string" || typeof payload.sessionId !== "string") {
      showDialog("The server sent an invalid authentication challenge.");
      return;
    }
    challenge = payload;
    attemptedSavedHash = false;
    pendingPasswordHash = null;
    submitButton.disabled = false;
    const savedHash = readSavedHash();
    if (savedHash && /^[0-9a-f]{64}$/.test(savedHash)) {
      attemptedSavedHash = true;
      try {
        sendProof(savedHash);
        return;
      } catch (error) {
        console.error("[Authentication] Failed to send saved credential:", error);
      }
    }
    showDialog(status.textContent);
  });

  ws.on("auth-result", payload => {
    if (payload.authenticated) {
      if (pendingPasswordHash && rememberInput.checked) {
        try {
          localStorage.setItem(PASSWORD_HASH_KEY, pendingPasswordHash);
        } catch (error) {
          console.warn("[Authentication] Browser could not remember the password hash:", error);
        }
      }
      pendingPasswordHash = null;
      passwordInput.value = "";
      if (dialog.open) dialog.close();
      return;
    }

    if (!challenge && payload.message) {
      status.textContent = payload.message;
      submitButton.disabled = true;
      showDialog(status.textContent);
      return;
    }

    if (attemptedSavedHash) {
      try {
        localStorage.removeItem(PASSWORD_HASH_KEY);
      } catch (error) {
        console.warn("[Authentication] Could not clear the rejected saved credential:", error);
      }
      attemptedSavedHash = false;
      status.textContent = "The saved credential was rejected. Enter the system password.";
    } else {
      status.textContent = "Password was not accepted. Try again.";
    }
    pendingPasswordHash = null;
    submitButton.disabled = false;
    showDialog(status.textContent);
  });

  ws.on("auth-status", payload => {
    if (payload.message) {
      console.warn("[Authentication] " + payload.message);
      status.textContent = payload.message;
      showDialog(status.textContent);
    }
  });

  ws.on("disconnected", () => {
    challenge = null;
    pendingPasswordHash = null;
    attemptedSavedHash = false;
  });

  form.addEventListener("submit", event => {
    event.preventDefault();
    if (!challenge || !passwordInput.value) return;
    try {
      pendingPasswordHash = hashPassword(passwordInput.value);
      attemptedSavedHash = false;
      sendProof(pendingPasswordHash);
      status.textContent = "Checking password…";
      submitButton.disabled = true;
    } catch (error) {
      status.textContent = error.message || "Unable to build authentication token.";
    }
  });

  cancelButton.addEventListener("click", () => {
    passwordInput.value = "";
    dialog.close();
  });

  dialog.addEventListener("close", () => {
    passwordInput.value = "";
    submitButton.disabled = false;
  });
}
