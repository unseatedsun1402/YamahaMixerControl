package MidiControl.Server;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import MidiControl.ControlServer.HardwareInputHandler;
import MidiControl.MidiDeviceManager.MidiIOManager;

public class MidiDebugMain {
    public static void main(String[] args) throws Exception {

        MidiServer server = new MidiServer();
        MidiIOManager io = server.getMidiDeviceManager();

        Logger root = Logger.getLogger("");
        root.setLevel(Level.FINE);
        for (Handler h : root.getHandlers()) {
            h.setLevel(Level.FINE);
        }

        // server.getCanonicalRegistry().enableDebug();
        HardwareInputHandler.enableDebug();
        MidiProcessingLoop.enableDebug();

        System.out.println("Available devices:");
        var devices = io.listDeviceDTOs();
        for (int i = 0; i < devices.size(); i++) {
            System.out.println(i + ": " + devices.get(i).name);
        }

        io.trySetOutputDevice(5); // example
        io.trySetInputDevice(14); // example

        server.run(); // <-- REQUIRED for input + SyncSend

        System.out.println("Listening for MIDI input...");
        Thread.sleep(Long.MAX_VALUE);
    }
}

