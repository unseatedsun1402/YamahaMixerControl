# YamahaMixerControl
    "Free for hobbyists, students, churches, and personal use.
     Commercial use requires a commercial license."
## ====== PREREQUISITES =====
The application runs as a self-contained Spring Boot executable JAR with an
embedded Tomcat server; installing or configuring Tomcat separately is no
longer required. Install JDK 18 or newer and make sure `java` is on your PATH.

To build the executable:

```powershell
.\gradlew.bat clean bootJar
```

Run it with:

```powershell
java -jar .\build\libs\YamahaMixerControl.jar
```

The application is available at http://127.0.0.1:8080/MidiControl. Stop it
with Ctrl+C; the MIDI server shuts down with the embedded web server.

## ===== Role Authentication ======

Each WebSocket client must select a role when it connects:

| Role | Access |
| --- | --- |
| Guest | View-only access; cannot change mixer controls or server settings |
| Operator | Can change input-to-mix send levels only |
| Admin | Unrestricted access to server commands |

Guest access does not require a password. Operator and admin password
credentials are read from `roles.json` in the user's MidiControl configuration
directory (on Windows, `%APPDATA%\MidiControl\roles.json`). The server creates
this file on first start with guest enabled and operator/admin disabled. No
default privileged passwords are shipped. The browser receives the enabled
roles from the server and marks unconfigured roles unavailable; the server
also rejects direct challenge requests for disabled roles.

Generate a password hash from a terminal using hidden password entry:

```powershell
java -jar .\build\libs\YamahaMixerControl.jar --generate-role-password operator
java -jar .\build\libs\YamahaMixerControl.jar --generate-role-password admin
```

For each command, copy its JSON object into the matching role in `roles.json`
and restart the server. Passwords must be at least 4 characters. The file
stores a unique random salt and PBKDF2-HMAC-SHA-256 password verifier, never
the plaintext password. Keep this file
readable only by the account running the server: in this challenge protocol,
the verifier is also sufficient to answer a fresh server challenge if stolen.
A different file location can be selected with the JVM property
`-Dmidicontrol.roles.file=<path>`.

The server issues a random authentication token after a successful login and
checks it for each request. The browser saves the token locally and silently
reauthenticates with it after a refresh. Tokens remain valid until the server
restarts; clearing the browser's local storage will require signing in again.
Because the token is a bearer credential, anyone who obtains it can act as that
role. Do not use the same password for operator and admin. Password changing
is not implemented yet; configure new values and restart the server to rotate
credentials.

For operator and admin sign-in, the server sends a one-time challenge with the
stored password salt and work factor. The browser derives the verifier using
PBKDF2-HMAC-SHA-256 and returns an HMAC proof; the password itself is never
sent. Challenges expire after one minute and are single-use. A captured proof
can be used to guess the password offline. A four-character PIN is intended
only as a deterrent against casual misuse, not strong security. Serve the
application over HTTPS (WSS); do not use plain HTTP/WebSocket on an untrusted
network. Browser cryptography requires a secure browser context:
HTTPS, or localhost during development. Privileged sign-in from another device
will not work over plain HTTP.

Token resumption also compares four client metadata checks: a browser-generated
random salt, the connection's peer IP address, and browser and operating-system
families derived from the WebSocket `User-Agent`. A token is invalidated after
two or more checks fail; one mismatch is tolerated for ordinary changes such
as a browser update or network address change. The browser persists the salt
alongside the token. This is a risk signal, not strong device authentication:
the `User-Agent` is client-controlled, and someone who copies both local
storage values can reproduce the salt. When deployed behind a reverse proxy,
the server sees the proxy's peer address unless trusted-proxy handling is
configured; do not trust arbitrary client-supplied forwarding headers.

## ===== Model Support =====
Currently this has been tested on both the Yamaha 01V96i and the Yamaha M7CL
for this to work make sure control change tx and rx are enabled and that the desk is set to NRPN mode for remote control and that **fader resolution is hi res**, _it used to be low_ but now I normalize these values in the program as this reduces the setup for the engineer/end user. The gui is using low res and then this is scaled up to be sent back to the mixer. I plan to support hi res later in the gui but in most contexts, remote mixing from a phone for in ear mixes this should be sufficient.
## ===== Usage ======

Load the index at http://<ip-address-of-server>:8080/MidiControl/settings.html
and use the selectors to assign the input and output interfaces of the server
to the Yamaha desk.

When the output is set you should be able to move faders and if the desk is receiving correctly faders on the desk should respond.

When the input is set and the server is correctly receiving and interpretting the data then the gui should respond on the mix (main.html) page.

Try moving some faders and see if it works

You can either edit the channels.json resource in the webapps/MidiControl area or use the change name to use the index of each channel (1-48) and rename them through this basic api tool.

### ===== Protected areas ======

I built this around serving multiple clients at once which is something that isn't possible with any of the first party older Yamaha Desks like the M7Cl or the LS9. There is no app from Yamaha for the 01V, or 01V96 V1/V2/V2i

Change the users in the conf area users.xml of the unpacked folder to suit your own needs. The mix pages are username and password protected to stop musicians from changing other peoples mixes, this can be disabled though if you take the page out of the auth group in the conf files.

## ====== SCREENSHOTS ======
<img width="3244" height="2329" alt="image" src="https://github.com/user-attachments/assets/e28ab26d-b102-486f-9d09-49debce5a708" />
above: default mix view

<img width="1226" height="1046" alt="image" src="https://github.com/user-attachments/assets/df260c30-ef08-496e-b219-868ccefa6c9a" />
above: edit view

<img width="3767" height="1772" alt="image" src="https://github.com/user-attachments/assets/4b0970f3-0190-4abd-b6a3-9011057d754a" />
above: settings page

<img width="1809" height="1065" alt="image" src="https://github.com/user-attachments/assets/6a58156c-f779-4ab8-97c5-1779122d3e18" />
above: navigation page
