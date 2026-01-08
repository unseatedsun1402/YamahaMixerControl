package MidiControl.TestUtilities;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import MidiControl.ControlServer.HardwareInputHandler;
import MidiControl.MidiDeviceManager.MidiIOManager;
import MidiControl.Server.MidiProcessingLoop;
import MidiControl.Server.MidiServer;

public class MidiDebugMain {
    public static void main(String[] args) throws Exception {

        MidiServer server = new MidiServer();
        MidiIOManager io = server.getMidiDeviceManager();

        Logger root = Logger.getLogger("");
        root.setLevel(Level.FINE);
        for (Handler h : root.getHandlers()) {
            h.setLevel(Level.INFO);
        }

        // server.getCanonicalRegistry().enableDebug();
        // HardwareInputHandler.enableDebug();
        // MidiProcessingLoop.enableDebug();

        System.out.println("Available devices:");
        var devices = io.listDeviceDTOs();
        for (int i = 0; i < devices.size(); i++) {
            System.out.println(i + ": " + devices.get(i).name);
        }

        io.trySetOutputDevice(4); // example
        io.trySetInputDevice(13); // example

        server.run(); // <-- REQUIRED for input + SyncSend
        server.RehydrateSever();

        System.out.println("Listening for MIDI input...");
        Thread.sleep(Long.MAX_VALUE);
    }
}

