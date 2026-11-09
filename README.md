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

For a local mixer UI preview, run:

```bat
tools\start-mixer-preview.cmd
```

The script reuses a responding preview on port 18080 or starts the app headlessly
with Gradle and prints the local `main.html` URL. Keep the terminal open while
using the preview.

For a Windows desktop launcher (recommended for file-browser launches), run:

```powershell
.\gradlew.bat packageWindowsApp
```

Launch `.\build\desktop\YamahaMixerControl\YamahaMixerControl.exe`. Keep the
entire `YamahaMixerControl` folder together when moving or distributing the
application. The status window shows the network address and the
`YamahaMixerControl.local` address when local network discovery is available.
Use **Exit** (or close the window) to stop the server. On networks that block
multicast DNS, use the displayed IP address instead.
If the server port is already in use, the launcher shows a dialog identifying
the port and advising you to close the other app.

To run the executable JAR directly, use `java -jar
.\build\libs\YamahaMixerControl.jar`; Windows will identify that process as
Java rather than YamahaMixerControl. The server is available at
http://127.0.0.1:8080/MidiControl and can also be stopped with Ctrl+C.
## ===== Model Support =====
Currently this has been tested on both the Yamaha 01V96i and the Yamaha M7CL
for this to work make sure control change tx and rx are enabled and that the desk is set to NRPN mode for remote control and that **fader resolution is hi res**, _it used to be low_ but now I normalize these values in the program as this reduces the setup for the engineer/end user. The gui is using low res and then this is scaled up to be sent back to the mixer. I plan to support hi res later in the gui but in most contexts, remote mixing from a phone for in ear mixes this should be sufficient.
## ===== M7CL SIL Simulator =====
For software-in-the-loop meter testing without a physical desk, select
**M7CL SIL Simulator** as both the MIDI input and output device in the settings
page, choose **YAMAHA_M7CL** as the console type, and apply the settings. The
simulator responds to M7CL meter SysEx requests and streams synthetic meter
values through the same MIDI input path used for hardware.

Meter clients can select the shared meter stream with a WebSocket request:
`requestMeters("INPUT", "IN_PRE_HPF", 0, 48)` for the M7CL. The arguments are
category, meter point/source, zero-based start channel, and channel count. M7CL
input meter points include `IN_PRE_HPF`, `IN_PRE_EQ`, `IN_POST_DYN2`,
`IN_DYN1_GR`, and `ALL_POINT`; 01V96i input sources include `CHANNEL`, `EQ_OUT`,
`COMP_OUT`, `COMP_GR`, `GATE_OUT`, `GATE_GR`, and `ALL`. The last client request
sets the active stream for all clients; keepalive refreshes reuse that
selection. Meter updates continue during state rehydration.
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

<img width="1634" height="1429" alt="image" src="https://github.com/user-attachments/assets/30c6d8dc-2a28-4fef-ae25-39e5dcbdd1cf" />
above: edit view

<img width="3760" height="1142" alt="image" src="https://github.com/user-attachments/assets/29b52e25-d4d2-4bbb-afc6-36b997d7ba71" />

above: settings page

<img width="1809" height="1065" alt="image" src="https://github.com/user-attachments/assets/6a58156c-f779-4ab8-97c5-1779122d3e18" />
above: navigation page
# Pipeline stress checks

Run the opt-in MIDI and WebSocket backpressure checks with:

```powershell
.\gradlew.bat stressTest
```

The MIDI matrix prints attempted, accepted, rejected, sent, pending, and peak
queue counts for SAFE_DIN and FAST_USB across short/SysEx-sized messages and
synthetic output delays. It also reports the producer rate actually achieved
by the test host; it does not connect to or certify a physical console.
Liveness probes remain active during rehydration and use a bounded priority
send lane, ahead of queued normal readback requests. A probe cannot interrupt a
MIDI message already being sent, and it still observes the configured transport
pacing.

The WebSocket check holds a fake client send open, floods meter updates, then
fills the reliable-message queue. This shows the effect of meter coalescing
separately from reliable event backlog.
