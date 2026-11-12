package MidiControl.HardwareClients;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;

import MidiControl.MidiDeviceManager.MidiDeviceUtils;
import MidiControl.MidiDeviceManager.MidiIOManager;

/**
 * Helper class for managing independent MIDI input/output pairs for hardware clients
 * Separate from the main Yamaha desk MidiIOManager to avoid interference
 */
public class MidiPortPair {
    
    private static final Logger logger = Logger.getLogger(MidiPortPair.class.getName());
    private static boolean debug = false;
    
    private final String inputDeviceName;
    private final String outputDeviceName;
    private final String clientId;
    
    private MidiDevice inputDevice;
    private MidiDevice outputDevice;
    private Transmitter transmitter;
    private Receiver receiver;
    
    private MidiMessageListener messageListener;
    
    private boolean isOpen = false;
    
    public MidiPortPair(String clientId, String inputDeviceName, String outputDeviceName) {
        this.clientId = clientId;
        this.inputDeviceName = inputDeviceName;
        this.outputDeviceName = outputDeviceName;
    }
    
    /**
     * Open the MIDI port pair
     */
    public synchronized void open() throws MidiUnavailableException {
        if (isOpen) {
            logger.warning("MIDI port pair already open for client: " + clientId);
            return;
        }
        
        // Find input device
        Optional<MidiDevice> inputOpt = MidiDeviceUtils.findDeviceByName(inputDeviceName, true);
        if (!inputOpt.isPresent()) {
            throw new MidiUnavailableException("Input device not found: " + inputDeviceName);
        }
        
        // Find output device
        Optional<MidiDevice> outputOpt = MidiDeviceUtils.findDeviceByName(outputDeviceName, false);
        if (!outputOpt.isPresent()) {
            throw new MidiUnavailableException("Output device not found: " + outputDeviceName);
        }
        
        inputDevice = inputOpt.get();
        outputDevice = outputOpt.get();
        
        // Open devices
        inputDevice.open();
        outputDevice.open();
        
        // Set up transmitter and receiver
        transmitter = inputDevice.getTransmitter();
        receiver = outputDevice.getReceiver();
        
        // Set up message listener if configured
        if (messageListener != null) {
            transmitter.setReceiver(new Receiver() {
                @Override
                public void send(MidiMessage message, long timeStamp) {
                    messageListener.onMidiMessage(message, timeStamp);
                }
                
                @Override
                public void close() {
                    // Nothing to close
                }
            });
        }
        
        isOpen = true;
        logger.info(String.format("Opened MIDI ports for %s: IN=%s, OUT=%s", 
            clientId, inputDeviceName, outputDeviceName));
    }
    
    /**
     * Close the MIDI port pair
     */
    public synchronized void close() {
        if (!isOpen) {
            return;
        }
        
        try {
            if (transmitter != null) {
                transmitter.close();
                transmitter = null;
            }
            
            if (receiver != null) {
                receiver.close();
                receiver = null;
            }
            
            if (inputDevice != null && inputDevice.isOpen()) {
                inputDevice.close();
                inputDevice = null;
            }
            
            if (outputDevice != null && outputDevice.isOpen()) {
                outputDevice.close();
                outputDevice = null;
            }
            
            isOpen = false;
            logger.info("Closed MIDI ports for client: " + clientId);
        } catch (Exception e) {
            logger.severe("Error closing MIDI ports for " + clientId + ": " + e.getMessage());
        }
    }
    
    /**
     * Send a MIDI message
     */
    public synchronized void send(MidiMessage message) {
        if (!isOpen || receiver == null) {
            logger.warning("Cannot send MIDI message - port not open for client: " + clientId);
            return;
        }
        
        try {
            receiver.send(message, -1);
            if (debug) {
                logger.fine(String.format("%s sent MIDI: %s", clientId, 
                    MidiDeviceUtils.messageToString(message)));
            }
        } catch (Exception e) {
            logger.severe("Failed to send MIDI message for " + clientId + ": " + e.getMessage());
        }
    }
    
    /**
     * Send raw MIDI bytes
     */
    public synchronized void send(byte[] data) {
        if (!isOpen || receiver == null) {
            return;
        }
        
        try {
            MidiMessage message = new javax.sound.midi.ShortMessage();
            if (data.length >= 3) {
                ((javax.sound.midi.ShortMessage)message).setMessage(data[0] & 0xFF, data[1] & 0xFF, data[2] & 0xFF);
            } else if (data.length == 2) {
                ((javax.sound.midi.ShortMessage)message).setMessage(data[0] & 0xFF, data[1] & 0xFF, 0);
            }
            receiver.send(message, -1);
        } catch (Exception e) {
            logger.severe("Failed to send raw MIDI for " + clientId + ": " + e.getMessage());
        }
    }
    
    /**
     * Set the MIDI message listener
     */
    public void setMessageListener(MidiMessageListener listener) {
        this.messageListener = listener;
        
        // If already open, update the transmitter's receiver
        if (isOpen && transmitter != null) {
            transmitter.setReceiver(new Receiver() {
                @Override
                public void send(MidiMessage message, long timeStamp) {
                    listener.onMidiMessage(message, timeStamp);
                }
                
                @Override
                public void close() {
                    // Nothing to close
                }
            });
        }
    }
    
    /**
     * Check if the port pair is open
     */
    public boolean isOpen() {
        return isOpen;
    }
    
    /**
     * Get the input device name
     */
    public String getInputDeviceName() {
        return inputDeviceName;
    }
    
    /**
     * Get the output device name
     */
    public String getOutputDeviceName() {
        return outputDeviceName;
    }
    
    /**
     * Get the client ID
     */
    public String getClientId() {
        return clientId;
    }
    
    /**
     * Interface for receiving MIDI messages
     */
    public interface MidiMessageListener {
        void onMidiMessage(MidiMessage message, long timeStamp);
    }
    
    public static void enableDebug() {
        debug = true;
        logger.info("MidiPortPair debug enabled");
    }
}