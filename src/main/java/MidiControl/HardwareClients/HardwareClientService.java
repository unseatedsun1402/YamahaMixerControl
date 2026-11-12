package MidiControl.HardwareClients;

import java.util.logging.Logger;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Server.MidiServer;

/**
 * Service for managing hardware clients within the MidiServer
 * This provides a clean interface for the MidiServer to interact with hardware clients
 */
public class HardwareClientService {
    
    private static final Logger logger = Logger.getLogger(HardwareClientService.class.getName());
    
    private final HardwareClientManager clientManager;
    private final CanonicalRegistry registry;
    private boolean initialized = false;
    
    public HardwareClientService(CanonicalRegistry registry) {
        this.registry = registry;
        this.clientManager = new HardwareClientManager(registry);
    }
    
    /**
     * Initialize the hardware client service
     */
    public void initialize() {
        if (initialized) {
            logger.warning("HardwareClientService already initialized");
            return;
        }
        
        logger.info("Initializing HardwareClientService");
        
        // Example: Create a default MCU client (in production, this would come from config)
        // try {
        //     MackieMCUClient mcuClient = new MackieMCUClient(
        //         "mcu-1",
        //         "Mackie Control Universal",
        //         "MCU Input",  // Would come from config
        //         "MCU Output", // Would come from config
        //         clientManager
        //     );
        //     clientManager.registerClient(mcuClient);
        // } catch (Exception e) {
        //     logger.warning("Failed to create default MCU client: " + e.getMessage());
        // }
        
        // Start all registered clients
        clientManager.startAll();
        
        initialized = true;
        logger.info("HardwareClientService initialized");
    }
    
    /**
     * Handle registry reload
     */
    public void onRegistryReload(CanonicalRegistry newRegistry) {
        logger.info("HardwareClientService handling registry reload");
        clientManager.onRegistryReload(newRegistry);
    }
    
    /**
     * Shutdown the hardware client service
     */
    public void shutdown() {
        logger.info("Shutting down HardwareClientService");
        clientManager.stopAll();
        initialized = false;
    }
    
    /**
     * Get the hardware client manager
     */
    public HardwareClientManager getClientManager() {
        return clientManager;
    }
    
    /**
     * Register a new hardware client
     */
    public void registerClient(IControlClient client) {
        clientManager.registerClient(client);
        if (initialized) {
            client.start();
        }
    }
    
    /**
     * Check if the service is initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
}