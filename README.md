# YamahaMixerControl
Java based service using midi to control Yamaha Digital mixers from a web-page interface.<br/>
<br/>---- prerequisites ----
Developed and deployed on tomcat server version 10.0.23
Compiled using jdk 21
Not all midi drivers are supported. If a device doesn't show up it might not be supported on your particular OS version. Most seem to work fine on Windows and MacOS but I had issues with cheap usb midi cables on Debian.
<br/>----------------------
<br/>Using the program requires a midi interface from the server computer to the mixer client. The device can be set from the project index webpage
<br/>If you need to make changes to suit your usage you will need a compiled war file to deploy it on a tomcat server. I usually place it in the root of the webapps folder as the paths are important to the functionality of the service.
