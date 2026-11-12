package MidiControl.HardwareClients;

import MidiControl.Controls.ControlInstance;
import MidiControl.Controls.CanonicalRegistry;

/**
 * Interface for hardware control surface clients (Mackie MCU, OSC, etc.)
 * Clients receive control value changes and can send control changes back to hardware
 */
public interface IControlClient {
    
    /**
     * Get the unique client identifier
     */
    String getClientId();
    
    /**
     * Get the display name for this client
     */
    String getDisplayName();
    
    /**
     * Get the client type (MCU, OSC, etc.)
     */
    ClientType getClientType();
    
    /**
     * Start the client connection to hardware
     */
    void start();
    
    /**
     * Stop the client connection
     */
    void stop();
    
    /**
     * Check if the client is currently running/connected
     */
    boolean isRunning();
    
    /**
     * Handle a control value change from the canonical model
     * @param canonicalId The canonical control ID
     * @param newValue The new control value
     * @param origin An identifier for the origin of this change (to prevent echo loops)
     */
    void handleControlChange(String canonicalId, int newValue, Object origin);
    
    /**
     * Handle a registry reload - clients need to reattach listeners
     */
    void onRegistryReload(CanonicalRegistry newRegistry);
    
    /**
     * Handle bank switching for strip-based clients (MCU, etc.)
     * @param bankType Type of bank (channels, buses, matrix, etc.)
     * @param bankIndex Bank index (0-based)
     * @param stripIndex Strip position (0-7 for MCU)
     */
    void handleBankChange(BankType bankType, int bankIndex, int stripIndex);
    
    /**
     * Types of hardware clients
     */
    enum ClientType {
        MCU("Mackie Control Universal"),
        HUI("Mackie HUI Protocol"),
        OSC("Open Sound Control"),
        MIDI_LEARN("Generic MIDI Learn"),
        CUSTOM("Custom Protocol");
        
        private final String displayName;
        
        ClientType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Bank types for strip-based controllers
     */
    enum BankType {
        CHANNELS("Channels"),
        BUSSES("Busses"),
        MATRIX("Matrix"),
        AUX("Aux"),
        OUTPUTS("Outputs"),
        MASTER("Master");
        
        private final String displayName;
        
        BankType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}