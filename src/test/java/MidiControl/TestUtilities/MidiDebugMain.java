package MidiControl.TestUtilities;

import java.util.ArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import MidiControl.MidiInputReceiver;
import MidiControl.ControlServer.HardwareInputHandler;
import MidiControl.MidiDeviceManager.MidiIOManager;
import MidiControl.MidiDeviceManager.ServerSettings;
import MidiControl.Server.MidiProcessingLoop;
import MidiControl.Server.MidiServer;
import MidiControl.SysexUtils.SysexParser;
import MidiControl.UserInterface.Meter.MeterRequest;

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
        // MidiInputReceiver.enableDebug();
        // MidiProcessingLoop.enableDebug();

        // System.out.println("Available devices:");
        // var devices = io.listDeviceDTOs();
        // for (int i = 0; i < devices.size(); i++) {
        //     System.out.println(i + ": " + devices.get(i).name);
        // }

        io.trySetOutputDevice(4); // example
        io.trySetInputDevice(13); // example

        server.run(); // <-- REQUIRED for input + SyncSend
        Thread.sleep(50);

        server.RehydrateSever();
        // MeterRequest request = (new MeterRequest(0, 0x1A, 0x0,0x2));
        // request.setChannelCount(5);
        // request.setStartChannel(14);
        // byte[] requestArray = request.toByteArray();
        // System.out.println("Requesting "+ SysexParser.bytesToHex(requestArray).toString());
        // server.getMidiDeviceManager().getMidiOut().sendMessage( requestArray );

        System.out.println("Listening for MIDI input...");
        Thread.sleep(8000);
    }
}

