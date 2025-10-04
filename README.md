# YamahaMixerControl
## ====== PREREQUISITES =====
You need to setup the tomcat server, and this project was previously compiled using the java jdk version 21 but I decided thats unnecessarily high, so it now compiles to support jdk 18. This can be downloaded either from oracle or open JDK. You want to set this as your JRE_HOME value. It can be higher if you have that installed but nothing lower. Please follow the Tomcat setup carefully including you environment variables. I have teststed using JAVA_HOME for java 1.8 and JRE_HOME version 18

Then you can either try to make changes if you want and build this yourself and deploy a custom version or use whatever is in the build/libs/ section and deploy what I have built. Use an ide with gradle support like netbeans and import this project and create the war file.

Take this war file, rename this to MidiControl.war (this matters for file path reasons) and drop it into the webapps area of the tomcat program filespace. This should unpack and you'll have a folder in the same directory with the same name. You dont need to remove the .war, in fact please dont remove it unless your are replacing it entirely.

Run the tomcat server and go to http://127.0.0.1:8080/MidiControl
## ===== Model Support =====
Currently this has been tested on both the Yamaha 01V96i and the Yamaha M7CL
for this to work make sure control change tx and rx are enabled and that the desk is set to Chanel table mode for remote control and that fader resolution is low not hi res. I plan to support hi res later but in most contexts, remote mixing from a phone for in ear mixes this should be sufficient.
## ===== Usage ======

Load the index at https://<ip-address-of-server>:8080/MidiControl and use the selectors to assign the input and output interfaces of the server to the yamaha desk

When the output is set you should be able to move faders and if the desk is receiving correctly faders on the desk should respond.

When the input is set and the server is correctly receiving and interpretting the data then the gui should respond.

Try moving some faders and see if it works

You can either edit the channels.json resource in the webapps/MidiControl area or use the change name to use the index of each channel (1-48) and rename them through this basic api tool.

### ===== Protected areas ======

I built this around serving multiple clients at once which is something that isn't possible with any of the first party older Yamaha Desks like the M7Cl or the LS9. There is no app from Yamaha for the 01V, or 01V96 V1/V2/V2i

Therefore for live use protected areas are probably wise.

Change the users in the conf area users.xml of the unpacked folder to suit your own needs. The mix pages are username and password protected to stop musicians from changing other peoples mixes, this can be disabled though if you take the page out of the auth group in the conf files.

## ====== SCREENSHOTS ======
<img width="3802" height="1740" alt="image" src="https://github.com/user-attachments/assets/869e9448-68ba-4251-a0b4-23db9d132d6c" />
Above: Main view where the foh is mixed, other views can use the url extension https://<ip-address-of-server>:8080/MidiControl/mix_<1-12>.html
