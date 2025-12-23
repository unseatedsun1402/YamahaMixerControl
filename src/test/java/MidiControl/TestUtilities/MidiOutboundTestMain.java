package MidiControl.TestUtilities;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import MidiControl.ControlServer.GuiInputHandler;
import MidiControl.ControlServer.HardwareInputHandler;
import MidiControl.ControlServer.OutputRouter;
import MidiControl.Controls.ControlInstance;
import MidiControl.MidiDeviceManager.MidiIOManager;
import MidiControl.MidiDeviceManager.ReceiverWrapper;
import MidiControl.MidiDeviceManager.TransportMode;
import MidiControl.Server.MidiServer;

public class MidiOutboundTestMain {

    public static void main(String[] args) throws Exception {

        MidiServer server = new MidiServer();
        MidiIOManager io = server.getMidiDeviceManager();

        // Enable debug logging
        Logger root = Logger.getLogger("");
        root.setLevel(Level.FINE);
        for (Handler h : root.getHandlers()) {
            h.setLevel(Level.FINE);
        }

        System.out.println("Available devices:");
        var devices = io.listDeviceDTOs();
        for (int i = 0; i < devices.size(); i++) {
            System.out.println(i + ": " + devices.get(i).name);
        }

        io.trySetOutputDevice(5); // example
        io.trySetInputDevice(14); // example

        server.run(); // REQUIRED for input + SyncSend

        OutputRouter outputRouter = new OutputRouter(server);
        GuiInputHandler gui = new GuiInputHandler(outputRouter);

        // Resolve the faders
        ControlInstance fader0 = server.getCanonicalRegistry().resolve("kInputFader.kFader.0");
        System.out.println("CHANGE FORMAT USED: " + fader0.getSysex().getParameter_change_format());
        ControlInstance fader1 = server.getCanonicalRegistry().resolve("kInputFader.kFader.4");
        

        if (fader0 == null || fader1 == null) {
            System.out.println("Could not resolve one or more faders");
            return;
        }

        

        System.out.println("Sending fader moves...");
        // GuiInputHandler.enableDebug();
        // OutputRouter.enableDebug();
        // ReceiverWrapper.enableDebug();
        HardwareInputHandler.enableDebug();

        // Sweep the faders up
        for (int v = 0; v <= 1023; v += 32) {
            io.setTransportMode(TransportMode.SYSEX);
            gui.handleGuiChange(fader0.getCanonicalId(), v);
            Thread.sleep(50);
            io.setTransportMode(TransportMode.NRPN);
            gui.handleGuiChange(fader1.getCanonicalId(), v);
        }

        // Sweep the faders down
        for (int v = 1023; v >= 0; v -= 32) {
            io.setTransportMode(TransportMode.SYSEX);
            gui.handleGuiChange(fader0.getCanonicalId(), v);
            Thread.sleep(50);
            io.setTransportMode(TransportMode.NRPN);
            gui.handleGuiChange(fader1.getCanonicalId(), v);
        }

        System.out.println("Done.");
        server.shutdown();
    }
}