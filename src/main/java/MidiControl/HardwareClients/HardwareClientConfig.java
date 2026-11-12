package MidiControl.HardwareClients;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for hardware clients
 * In a real implementation, this would be loaded from a JSON/YAML config file
 */
public class HardwareClientConfig {
    
    private List<ClientConfig> clients = new ArrayList<>();
    
    public HardwareClientConfig() {
        // Default configuration
        addDefaultConfig();
    }
    
    private void addDefaultConfig() {
        // Example MCU configuration
        ClientConfig mcuConfig = new ClientConfig();
        mcuConfig.setId("mcu-1");
        mcuConfig.setDisplayName("Mackie Control Universal");
        mcuConfig.setType(IControlClient.ClientType.MCU);
        mcuConfig.setEnabled(false); // Disabled by default
        mcuConfig.setInputDeviceName("MCU Input");
        mcuConfig.setOutputDeviceName("MCU Output");
        mcuConfig.setBankType(IControlClient.BankType.CHANNELS);
        mcuConfig.setStartBankIndex(0);
        
        clients.add(mcuConfig);
        
        // Example OSC configuration
        ClientConfig oscConfig = new ClientConfig();
        oscConfig.setId("osc-1");
        oscConfig.setDisplayName("OSC Client");
        oscConfig.setType(IControlClient.ClientType.OSC);
        oscConfig.setEnabled(false);
        oscConfig.setHost("localhost");
        oscConfig.setPort(9000);
        
        clients.add(oscConfig);
    }
    
    public List<ClientConfig> getClients() {
        return clients;
    }
    
    public void setClients(List<ClientConfig> clients) {
        this.clients = clients;
    }
    
    public List<ClientConfig> getEnabledClients() {
        return clients.stream()
            .filter(ClientConfig::isEnabled)
            .toList();
    }
    
    /**
     * Individual client configuration
     */
    public static class ClientConfig {
        private String id;
        private String displayName;
        private IControlClient.ClientType type;
        private boolean enabled;
        
        // MIDI-specific
        private String inputDeviceName;
        private String outputDeviceName;
        
        // OSC-specific
        private String host;
        private int port;
        
        // Bank configuration
        private IControlClient.BankType bankType;
        private int startBankIndex;
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
        
        public IControlClient.ClientType getType() {
            return type;
        }
        
        public void setType(IControlClient.ClientType type) {
            this.type = type;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getInputDeviceName() {
            return inputDeviceName;
        }
        
        public void setInputDeviceName(String inputDeviceName) {
            this.inputDeviceName = inputDeviceName;
        }
        
        public String getOutputDeviceName() {
            return outputDeviceName;
        }
        
        public void setOutputDeviceName(String outputDeviceName) {
            this.outputDeviceName = outputDeviceName;
        }
        
        public String getHost() {
            return host;
        }
        
        public void setHost(String host) {
            this.host = host;
        }
        
        public int getPort() {
            return port;
        }
        
        public void setPort(int port) {
            this.port = port;
        }
        
        public IControlClient.BankType getBankType() {
            return bankType;
        }
        
        public void setBankType(IControlClient.BankType bankType) {
            this.bankType = bankType;
        }
        
        public int getStartBankIndex() {
            return startBankIndex;
        }
        
        public void setStartBankIndex(int startBankIndex) {
            this.startBankIndex = startBankIndex;
        }
    }
}