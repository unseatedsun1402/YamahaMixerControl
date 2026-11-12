package examples;

import MidiControl.HardwareClients.*;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;

import java.util.List;
import java.util.logging.Logger;

/**
 * Example showing how to integrate the hardware client framework
 * This demonstrates the complete flow from hardware input to software and back
 */
public class HardwareClientIntegrationExample {
    
    private static final Logger logger = Logger.getLogger(HardwareClientIntegrationExample.class.getName());
    
    public static void main(String[] args) {
        logger.info("Starting Hardware Client Integration Example");
        
        try {
            // 1. Create a canonical registry (simplified version)
            List<SysexMapping> sysexMappings = SysexMappingLoader.loadMappingsFromResource(
                "MidiControl/01v96i_sysex_mappings.json");
            CanonicalRegistry registry = new CanonicalRegistry(sysexMappings, new SysexParser(sysexMappings));
            
            // 2. Create hardware client manager
            HardwareClientManager clientManager = new HardwareClientManager(registry);
            
            // 3. Create MCU client (simulated - would use real MIDI devices in production)
            MackieMCUClient mcuClient = new MackieMCUClient(
                "mcu-1",
                "Mackie Control Universal (Simulated)",
                "Simulated MCU Input",
                "Simulated MCU Output",
                clientManager
            );
            
            // 4. Register and start the client
            clientManager.registerClient(mcuClient);
            clientManager.startAll();
            
            logger.info("Hardware client framework initialized");
            logger.info("Registered client: " + mcuClient.getDisplayName());
            
            // 5. Simulate hardware input (fader move on strip 0)
            logger.info("Simulating hardware input: MCU fader 0 moved to 50%");
            clientManager.handleHardwareInput("mcu-1", "channel.1.level", 64); // 64 = 50% of 127
            
            // 6. Simulate bank change
            logger.info("Simulating bank change: Switching to channels bank 1");
            clientManager.handleHardwareBankChange("mcu-1", 
                IControlClient.BankType.CHANNELS, 1, 0);
            
            // 7. Simulate software-initiated change (e.g., from web interface)
            logger.info("Simulating software change: Channel 1 level changed to 90%");
            ControlInstance channel1Level = registry.resolveCanonicalId("channel.1.level");
            if (channel1Level != null) {
                // This will trigger HardwareClientManager.onControlChanged()
                // which will send the update to the MCU client (if not the origin)
                channel1Level.updateValue(115); // 115 ≈ 90% of 127
            }
            
            // 8. Simulate registry reload (e.g., desk model changed)
            logger.info("Simulating registry reload");
            clientManager.onRegistryReload(registry);
            
            // 9. Clean shutdown
            logger.info("Shutting down hardware clients");
            clientManager.stopAll();
            
            logger.info("Example completed successfully");
            
        } catch (Exception e) {
            logger.severe("Error in example: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Example of creating a hardware client service for MidiServer integration
     */
    public static class MidiServerIntegrationExample {
        
        private HardwareClientService hardwareService;
        
        public void initialize(CanonicalRegistry registry) {
            // Create and initialize hardware service
            hardwareService = new HardwareClientService(registry);
            hardwareService.initialize();
            
            // Register configured clients from config
            HardwareClientConfig config = new HardwareClientConfig();
            
            for (HardwareClientConfig.ClientConfig clientConfig : config.getEnabledClients()) {
                if (clientConfig.getType() == IControlClient.ClientType.MCU) {
                    MackieMCUClient mcuClient = new MackieMCUClient(
                        clientConfig.getId(),
                        clientConfig.getDisplayName(),
                        clientConfig.getInputDeviceName(),
                        clientConfig.getOutputDeviceName(),
                        hardwareService.getClientManager()
                    );
                    hardwareService.registerClient(mcuClient);
                }
                // Add other client types (OSC, etc.) here
            }
        }
        
        public void onRegistryReload(CanonicalRegistry newRegistry) {
            if (hardwareService != null) {
                hardwareService.onRegistryReload(newRegistry);
            }
        }
        
        public void shutdown() {
            if (hardwareService != null) {
                hardwareService.shutdown();
            }
        }
    }
}
