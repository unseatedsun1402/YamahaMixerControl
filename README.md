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
