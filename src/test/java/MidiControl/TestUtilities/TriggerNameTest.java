package MidiControl.TestUtilities;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import MidiControl.ControlServer.GuiInputHandler;
import MidiControl.Controls.ControlInstance;
import MidiControl.MidiDeviceManager.MidiIOManager;
import MidiControl.MidiDeviceManager.MidiInput;
import MidiControl.MidiDeviceManager.MidiInputReceiver;
import MidiControl.MidiDeviceManager.MidiOutput;
import MidiControl.MidiDeviceManager.TransportMode;
import MidiControl.Routing.OutputRouter;
import MidiControl.Server.MidiServer;

public class TriggerNameTest {

    public static void main(String[] args) throws Exception {

        MidiServer server = new MidiServer();
        MidiIOManager io = server.getMidiDeviceManager();

        // Enable debug logging
        Logger root = Logger.getLogger("");
        root.setLevel(Level.FINE);
        for (Handler h : root.getHandlers()) {
            h.setLevel(Level.INFO);
        }

        System.out.println("Available devices:");
        var devices = io.listDeviceDTOs();
        for (int i = 0; i < devices.size(); i++) {
            System.out.println(i + ": " + devices.get(i).name);
        }

        io.trySetOutputDevice(4); // example
        io.trySetInputDevice(13); // example

        server.run(); // REQUIRED for input + SyncSend

        OutputRouter outputRouter = new OutputRouter(server.getCanonicalRegistry(),io);
        GuiInputHandler gui = new GuiInputHandler(outputRouter);

        // Resolve the faders
        ControlInstance char1 = server.getCanonicalRegistry().resolve("kNameInputChannel.kNameShort1.0");
        ControlInstance char2 = server.getCanonicalRegistry().resolve("kNameInputChannel.kNameShort2.0");
        

        if (char1 == null || char2 == null) {
            System.out.println("Could not resolve one or more faders");
            return;
        }

        MidiInputReceiver.enableDebug();

        System.out.println("Requesting Channel 1 name");
        byte[] char1request = char1.getSysex().buildRequestMessage(0);
        io.sendAsync(char1request);

        System.out.println("Requesting Channel 2 name");
        byte[] char2request = char1.getSysex().buildRequestMessage(0);
        io.sendAsync(char2request);

        System.out.println("Done.");
        server.shutdown();
    }
}