package MidiControl.Server;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import javax.sound.midi.MidiMessage;

import MidiControl.ControlServer.CanonicalInputEvent;
import MidiControl.ControlServer.HardwareInputHandler;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;
import MidiControl.SysexUtils.SysexParser;
import MidiControl.UserInterface.Frontend.GuiBroadcastListener;

public class MidiProcessingLoop implements Runnable {

    private final ConcurrentLinkedQueue<MidiMessage> inputBuffer;
    private final HardwareInputHandler inputHandler;
    private final CanonicalRegistry canonicalRegistry;
    private final GuiBroadcastListener guiBroadcastListener;

    private static final Logger logger = Logger.getLogger(MidiProcessingLoop.class.getName());
    private static boolean debug = false;
    private static boolean shutdownFlag = false;

    public static void enableDebug() {
        debug = true;
        logger.info("Debug Logs On");
    }

    public static void shutdown(){
        shutdownFlag = true;
    }


    public MidiProcessingLoop(
            ConcurrentLinkedQueue<MidiMessage> inputBuffer,
            HardwareInputHandler inputHandler,
            CanonicalRegistry canonicalRegistry,
            GuiBroadcastListener listener
    ) {
        this.inputBuffer = inputBuffer;
        this.inputHandler = inputHandler;
        this.canonicalRegistry = canonicalRegistry;
        this.guiBroadcastListener = listener;
    }

    @Override
    public void run() {
        long lastHeartbeat = System.currentTimeMillis();
        while (!Thread.currentThread().isInterrupted()) {
            if (shutdownFlag){Thread.currentThread().interrupt(); continue;}
            processIncomingMidi();

            if (System.currentTimeMillis() - lastHeartbeat > 5000) {
                logger.info("Processing loop heartbeat: still alive");
                lastHeartbeat = System.currentTimeMillis();
            }

            try {
                Thread.sleep(2);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void processIncomingMidi() {
        while (!inputBuffer.isEmpty()) {
            MidiMessage msg = inputBuffer.poll();
            if (msg == null) continue;

            try {
                if(debug){logger.fine("Received MIDI: " + SysexParser.bytesToHex(msg.getMessage()));}

                CanonicalInputEvent event = inputHandler.handle(msg);
                if (event == null) continue;

                ControlInstance instance = canonicalRegistry.resolve(event);
                if (instance == null) {
                    if(debug){logger.warning("No control resolved for event: " + event.getType());}
                    continue;
                }

                int eventValue = instance.extractValue(event);
                instance.updateValue(eventValue);

                if(debug){logger.fine("Updated " + instance.getCanonicalId() + " = " + eventValue);}

                guiBroadcastListener.onControlChanged(instance, eventValue);

            } catch (Exception ex) {
                logger.warning("Error processing MIDI message: " + ex.getMessage());
                if(debug){logger.finer("Stack trace: " + ex);}
            }
        }
    }
}