package MidiControl.HardwareClients;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;

/**
 * Mackie Control Universal (MCU) hardware client
 * Uses pitch bend for faders (channels 0-7), CC for mutes/solos, and bank switching
 */
public class MackieMCUClient implements IControlClient, MidiPortPair.MidiMessageListener {
    
    private static final Logger logger = Logger.getLogger(MackieMCUClient.class.getName());
    private static boolean debug = false;
    
    private final String clientId;
    private final String displayName;
    private final MidiPortPair midiPorts;
    private final HardwareClientManager clientManager;
    
    private CanonicalRegistry registry;
    private final Map<String, Integer> controlCache = new ConcurrentHashMap<>();
    
    // MCU Protocol Constants
    private static final int MCU_FADER_CHANNELS = 8;  // 8 faders per bank
    private static final int MCU_CHANNEL_BASE = 0;    // Channels start at MIDI channel 0
    
    // Pitch Bend for faders (MCU uses pitch bend, not HUI)
    private static final int MCU_FADER_PITCH_BEND = 0xE0;  // Status byte for pitch bend
    
    // Note On/Off for buttons (MCU)
    private static final int MCU_NOTE_ON = 0x90;
    private static final int MCU_NOTE_OFF = 0x80;
    private static final int MCU_CC = 0xB0;
    
    // MCU Button mappings (simplified - real MCU has more)
    private static final int MCU_MUTE_BASE_NOTE = 0x10;   // Note numbers for mute buttons
    private static final int MCU_SOLO_BASE_NOTE = 0x18;   // Note numbers for solo buttons
    private static final int MCU_SELECT_BASE_NOTE = 0x20; // Note numbers for select buttons
    
    // Bank switching CCs (MCU uses CC 0x20-0x27 for bank select)
    private static final int MCU_BANK_SELECT_CC = 0x20;
    
    // Current bank state
    private BankType currentBankType = BankType.CHANNELS;
    private int currentBankIndex = 0;
    private final Map<Integer, String> stripToCanonicalMap = new HashMap<>();
    
    // Running state
    private boolean running = false;
    
    public MackieMCUClient(String clientId, String displayName, 
                          String inputDeviceName, String outputDeviceName,
                          HardwareClientManager clientManager) {
        this.clientId = clientId;
        this.displayName = displayName;
        this.clientManager = clientManager;
        this.midiPorts = new MidiPortPair(clientId, inputDeviceName, outputDeviceName);
        this.midiPorts.setMessageListener(this);
    }
    
    @Override
    public String getClientId() {
        return clientId;
    }
    
    @Override
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public ClientType getClientType() {
        return ClientType.MCU;
    }
    
    @Override
    public void start() {
        if (running) {
            logger.warning("MCU client already running: " + clientId);
            return;
        }
        
        try {
            midiPorts.open();
            running = true;
            logger.info("MCU client started: " + displayName);
            
            // Initialize bank mapping
            updateBankMapping();
            
            // Send initial fader positions for current bank
            sendCurrentBankValues();
            
        } catch (Exception e) {
            logger.severe("Failed to start MCU client " + clientId + ": " + e.getMessage());
            running = false;
        }
    }
    
    @Override
    public void stop() {
        if (!running) {
            return;
        }
        
        try {
            midiPorts.close();
            running = false;
            logger.info("MCU client stopped: " + displayName);
        } catch (Exception e) {
            logger.severe("Error stopping MCU client " + clientId + ": " + e.getMessage());
        }
    }
    
    @Override
    public boolean isRunning() {
        return running;
    }
    
    @Override
    public void handleControlChange(String canonicalId, int newValue, Object origin) {
        if (!running || origin == this) {
            // Skip if not running or if we originated this change
            return;
        }
        
        // Cache the value
        controlCache.put(canonicalId, newValue);
        
        // Check if this control is in our current bank mapping
        for (Map.Entry<Integer, String> entry : stripToCanonicalMap.entrySet()) {
            if (entry.getValue().equals(canonicalId)) {
                int stripIndex = entry.getKey();
                sendControlToHardware(canonicalId, newValue, stripIndex);
                return;
            }
        }
        
        // If not in current bank, check if it's a master or special control
        sendSpecialControlToHardware(canonicalId, newValue);
    }
    
    @Override
    public void onRegistryReload(CanonicalRegistry newRegistry) {
        this.registry = newRegistry;
        controlCache.clear();
        updateBankMapping();
        
        if (running) {
            sendCurrentBankValues();
        }
        
        logger.info("MCU client reloaded registry for desk: " + 
            (newRegistry != null ? newRegistry.getDeskType() : "null"));
    }
    
    @Override
    public void handleBankChange(BankType bankType, int bankIndex, int stripIndex) {
        if (bankType != currentBankType || bankIndex != currentBankIndex) {
            currentBankType = bankType;
            currentBankIndex = bankIndex;
            updateBankMapping();
            
            if (running) {
                sendCurrentBankValues();
            }
            
            logger.info(String.format("MCU bank changed to %s bank %d", 
                bankType.getDisplayName(), bankIndex));
        }
    }
    
    @Override
    public void onMidiMessage(MidiMessage message, long timeStamp) {
        if (!running) {
            return;
        }
        
        try {
            byte[] data = message.getMessage();
            if (data.length < 3) {
                return;
            }
            
            int status = data[0] & 0xFF;
            int data1 = data[1] & 0xFF;
            int data2 = data[2] & 0xFF;
            
            if (debug) {
                logger.fine(String.format("MCU MIDI IN: status=0x%02X, data1=0x%02X, data2=0x%02X", 
                    status, data1, data2));
            }
            
            // Handle pitch bend for faders (MCU faders)
            if ((status & 0xF0) == MCU_FADER_PITCH_BEND) {
                int channel = status & 0x0F;
                if (channel < MCU_FADER_CHANNELS) {
                    handleFaderMove(channel, data1, data2);
                }
            }
            // Handle note on/off for buttons
            else if ((status & 0xF0) == MCU_NOTE_ON || (status & 0xF0) == MCU_NOTE_OFF) {
                int channel = status & 0x0F;
                boolean pressed = (status & 0xF0) == MCU_NOTE_ON && data2 > 0;
                handleButtonPress(channel, data1, pressed);
            }
            // Handle CC for bank switching
            else if ((status & 0xF0) == MCU_CC) {
                int channel = status & 0x0F;
                handleControlChange(channel, data1, data2);
            }
            
        } catch (Exception e) {
            logger.severe("Error processing MCU MIDI message: " + e.getMessage());
        }
    }
    
    private void handleFaderMove(int stripIndex, int lsb, int msb) {
        // MCU faders use 14-bit pitch bend: lsb (0-127), msb (0-127)
        // Combine to 14-bit value: value = (msb << 7) | lsb
        int rawValue = ((msb & 0x7F) << 7) | (lsb & 0x7F);
        
        // Convert to 0-16383 range (14-bit)
        int normalizedValue = rawValue;
        
        // Map to canonical value (0-127 for most faders)
        int canonicalValue = normalizeMCUToCanonical(normalizedValue, 0, 16383, 0, 127);
        
        String canonicalId = stripToCanonicalMap.get(stripIndex);
        if (canonicalId != null) {
            if (debug) {
                logger.fine(String.format("MCU fader %d -> %s = %d (raw 0x%04X)", 
                    stripIndex, canonicalId, canonicalValue, rawValue));
            }
            
            // Forward to client manager (which will update canonical model)
            clientManager.handleHardwareInput(clientId, canonicalId, canonicalValue);
        }
    }
    
    private void handleButtonPress(int channel, int note, boolean pressed) {
        int stripIndex = channel;  // Channel maps to strip
        
        // Determine button type based on note number
        if (note >= MCU_MUTE_BASE_NOTE && note < MCU_MUTE_BASE_NOTE + MCU_FADER_CHANNELS) {
            int buttonIndex = note - MCU_MUTE_BASE_NOTE;
            handleMuteButton(stripIndex, buttonIndex, pressed);
        } 
        else if (note >= MCU_SOLO_BASE_NOTE && note < MCU_SOLO_BASE_NOTE + MCU_FADER_CHANNELS) {
            int buttonIndex = note - MCU_SOLO_BASE_NOTE;
            handleSoloButton(stripIndex, buttonIndex, pressed);
        }
        else if (note >= MCU_SELECT_BASE_NOTE && note < MCU_SELECT_BASE_NOTE + MCU_FADER_CHANNELS) {
            int buttonIndex = note - MCU_SELECT_BASE_NOTE;
            handleSelectButton(stripIndex, buttonIndex, pressed);
        }
    }
    
    private void handleMuteButton(int stripIndex, int buttonIndex, boolean pressed) {
        // Mute buttons toggle on press (not on release)
        if (!pressed) {
            return;
        }
        
        String canonicalId = stripToCanonicalMap.get(stripIndex);
        if (canonicalId != null) {
            // Find the mute control for this channel
            String muteCanonicalId = findMuteControlId(canonicalId);
            if (muteCanonicalId != null) {
                // Toggle mute (0 = unmuted, 1 = muted)
                Integer currentValue = controlCache.get(muteCanonicalId);
                int newValue = (currentValue == null || currentValue == 0) ? 1 : 0;
                
                if (debug) {
                    logger.fine(String.format("MCU mute button %d -> %s = %d", 
                        stripIndex, muteCanonicalId, newValue));
                }
                
                clientManager.handleHardwareInput(clientId, muteCanonicalId, newValue);
            }
        }
    }
    
    private void handleSoloButton(int stripIndex, int buttonIndex, boolean pressed) {
        // Similar to mute but for solo
        if (!pressed) {
            return;
        }
        
        String canonicalId = stripToCanonicalMap.get(stripIndex);
        if (canonicalId != null) {
            String soloCanonicalId = findSoloControlId(canonicalId);
            if (soloCanonicalId != null) {
                Integer currentValue = controlCache.get(soloCanonicalId);
                int newValue = (currentValue == null || currentValue == 0) ? 1 : 0;
                
                if (debug) {
                    logger.fine(String.format("MCU solo button %d -> %s = %d", 
                        stripIndex, soloCanonicalId, newValue));
                }
                
                clientManager.handleHardwareInput(clientId, soloCanonicalId, newValue);
            }
        }
    }
    
    private void handleSelectButton(int stripIndex, int buttonIndex, boolean pressed) {
        // Select button logic would go here
        // This would typically highlight the channel in software
        logger.fine("MCU select button pressed on strip " + stripIndex);
    }
    
    private void handleControlChange(int channel, int cc, int value) {
        // Bank switching CCs
        if (cc >= MCU_BANK_SELECT_CC && cc < MCU_BANK_SELECT_CC + MCU_FADER_CHANNELS) {
            int bankDelta = value >= 64 ? value - 128 : value;  // Convert to signed
            int newBankIndex = Math.max(0, currentBankIndex + bankDelta);
            
            // Notify client manager of bank change
            clientManager.handleHardwareBankChange(clientId, currentBankType, newBankIndex, 0);
        }
    }
    
    private void sendControlToHardware(String canonicalId, int newValue, int stripIndex) {
        if (!running) {
            return;
        }
        
        try {
            // For faders, send pitch bend
            if (canonicalId.contains(".level") || canonicalId.contains(".fader")) {
                // Convert canonical value (0-127) to MCU 14-bit (0-16383)
                int mcuValue = normalizeCanonicalToMCU(newValue, 0, 127, 0, 16383);
                
                // Split into LSB and MSB
                int lsb = mcuValue & 0x7F;
                int msb = (mcuValue >> 7) & 0x7F;
                
                // Send pitch bend message
                int status = MCU_FADER_PITCH_BEND | (stripIndex & 0x0F);
                sendMidi(status, lsb, msb);
                
                if (debug) {
                    logger.fine(String.format("MCU fader update: strip %d = %d -> 0x%04X", 
                        stripIndex, newValue, mcuValue));
                }
            }
            // For mute/solo buttons
            else if (canonicalId.contains(".mute")) {
                // Mute buttons use note on/off with velocity
                int note = MCU_MUTE_BASE_NOTE + stripIndex;
                int velocity = newValue > 0 ? 127 : 0;  // 127 = LED on, 0 = LED off
                int status = velocity > 0 ? MCU_NOTE_ON : MCU_NOTE_OFF;
                status |= (stripIndex & 0x0F);
                
                sendMidi(status, note, velocity);
            }
            else if (canonicalId.contains(".solo")) {
                // Solo buttons
                int note = MCU_SOLO_BASE_NOTE + stripIndex;
                int velocity = newValue > 0 ? 127 : 0;
                int status = velocity > 0 ? MCU_NOTE_ON : MCU_NOTE_OFF;
                status |= (stripIndex & 0x0F);
                
                sendMidi(status, note, velocity);
            }
            
        } catch (Exception e) {
            logger.severe("Failed to send control to MCU hardware: " + e.getMessage());
        }
    }
    
    private void sendSpecialControlToHardware(String canonicalId, int newValue) {
        // Handle master fader, transport controls, etc.
        // For now, just log
        if (debug) {
            logger.fine("Special control update (not in current bank): " + canonicalId + " = " + newValue);
        }
    }
    
    private void sendCurrentBankValues() {
        if (!running || registry == null) {
            return;
        }
        
        logger.info("Sending current bank values to MCU");
        
        for (Map.Entry<Integer, String> entry : stripToCanonicalMap.entrySet()) {
            int stripIndex = entry.getKey();
            String canonicalId = entry.getValue();
            
            // Get current value from cache or registry
            Integer cachedValue = controlCache.get(canonicalId);
            if (cachedValue == null) {
                ControlInstance ci = registry.resolveCanonicalId(canonicalId);
                if (ci != null) {
                    cachedValue = ci.getValue();
                    controlCache.put(canonicalId, cachedValue);
                }
            }
            
            if (cachedValue != null) {
                sendControlToHardware(canonicalId, cachedValue, stripIndex);
            }
        }
    }
    
    private void updateBankMapping() {
        stripToCanonicalMap.clear();
        
        if (registry == null) {
            return;
        }
        
        // Map strips 0-7 to canonical controls based on bank type and index
        int startIndex = currentBankIndex * MCU_FADER_CHANNELS;
        
        for (int strip = 0; strip < MCU_FADER_CHANNELS; strip++) {
            int canonicalIndex = startIndex + strip;
            String canonicalId = buildCanonicalIdForBank(canonicalIndex);
            
            if (canonicalId != null) {
                stripToCanonicalMap.put(strip, canonicalId);
                
                if (debug) {
                    logger.fine(String.format("MCU strip %d -> %s", strip, canonicalId));
                }
            }
        }
    }
    
    private String buildCanonicalIdForBank(int index) {
        // Build canonical ID based on bank type
        switch (currentBankType) {
            case CHANNELS:
                return String.format("channel.%d.level", index + 1);  // Channels are 1-based
            case BUSSES:
                return String.format("bus.%d.level", index + 1);
            case MATRIX:
                return String.format("matrix.%d.level", index + 1);
            case AUX:
                return String.format("aux.%d.level", index + 1);
            case OUTPUTS:
                return String.format("output.%d.level", index + 1);
            case MASTER:
                return "master.level";  // Master is single fader
            default:
                return null;
        }
    }
    
    private String findMuteControlId(String levelCanonicalId) {
        // Convert "channel.X.level" to "channel.X.mute"
        if (levelCanonicalId.contains(".level")) {
            return levelCanonicalId.replace(".level", ".mute");
        }
        return null;
    }
    
    private String findSoloControlId(String levelCanonicalId) {
        // Convert "channel.X.level" to "channel.X.solo"
        if (levelCanonicalId.contains(".level")) {
            return levelCanonicalId.replace(".level", ".solo");
        }
        return null;
    }
    
    private int normalizeMCUToCanonical(int mcuValue, int mcuMin, int mcuMax, int canonMin, int canonMax) {
        if (mcuMax == mcuMin) return canonMin;
        double normalized = (double)(mcuValue - mcuMin) / (mcuMax - mcuMin);
        return canonMin + (int)Math.round(normalized * (canonMax - canonMin));
    }
    
    private int normalizeCanonicalToMCU(int canonValue, int canonMin, int canonMax, int mcuMin, int mcuMax) {
        if (canonMax == canonMin) return mcuMin;
        double normalized = (double)(canonValue - canonMin) / (canonMax - canonMin);
        return mcuMin + (int)Math.round(normalized * (mcuMax - mcuMin));
    }
    
    private void sendMidi(int status, int data1, int data2) {
        try {
            byte[] msg = new byte[] {
                (byte)(status & 0xFF),
                (byte)(data1 & 0xFF),
                (byte)(data2 & 0xFF)
            };
            midiPorts.send(msg);
        } catch (Exception e) {
            logger.severe("Failed to send MIDI message: " + e.getMessage());
        }
    }
    
    public static void enableDebug() {
        debug = true;
        logger.info("MackieMCUClient debug enabled");
    }
}