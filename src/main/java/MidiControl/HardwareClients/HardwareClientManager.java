package MidiControl.HardwareClients;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;
import MidiControl.Controls.ControlListener;

/**
 * Manages hardware client adapters and dispatches control changes
 * Uses thread-local origin tracking to prevent echo loops
 */
public class HardwareClientManager implements ControlListener {
    
    private static final Logger logger = Logger.getLogger(HardwareClientManager.class.getName());
    private static boolean debug = false;
    
    private final Map<String, IControlClient> clients = new ConcurrentHashMap<>();
    private final Map<String, ThreadLocal<Object>> clientOrigins = new ConcurrentHashMap<>();
    private final CanonicalRegistry registry;
    
    // Registry reload listener to survive canonical registry rebuilds
    private final ControlListener registryReloadListener = new ControlListener() {
        @Override
        public void onControlChanged(ControlInstance instance, int newValue) {
            // This listener ensures we survive registry reloads
            logger.fine("Registry reload detected via control change");
        }
    };
    
    public HardwareClientManager(CanonicalRegistry registry) {
        this.registry = registry;
        
        // Attach to all control instances for dispatching
        attachToRegistry();
    }
    
    /**
     * Register a new hardware client
     */
    public void registerClient(IControlClient client) {
        String clientId = client.getClientId();
        
        if (clients.containsKey(clientId)) {
            logger.warning("Client already registered: " + clientId);
            return;
        }
        
        clients.put(clientId, client);
        clientOrigins.put(clientId, new ThreadLocal<>());
        
        logger.info(String.format("Registered %s client: %s", 
            client.getClientType().getDisplayName(), client.getDisplayName()));
        
        // If registry is already loaded, notify the client
        if (registry != null) {
            client.onRegistryReload(registry);
        }
    }
    
    /**
     * Unregister a hardware client
     */
    public void unregisterClient(String clientId) {
        IControlClient client = clients.remove(clientId);
        if (client != null) {
            clientOrigins.remove(clientId);
            client.stop();
            logger.info("Unregistered client: " + clientId);
        }
    }
    
    /**
     * Get all registered clients
     */
    public List<IControlClient> getAllClients() {
        return new ArrayList<>(clients.values());
    }
    
    /**
     * Get a client by ID
     */
    public IControlClient getClient(String clientId) {
        return clients.get(clientId);
    }
    
    /**
     * Start all registered clients
     */
    public void startAll() {
        for (IControlClient client : clients.values()) {
            try {
                client.start();
            } catch (Exception e) {
                logger.severe("Failed to start client " + client.getClientId() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Stop all registered clients
     */
    public void stopAll() {
        for (IControlClient client : clients.values()) {
            try {
                client.stop();
            } catch (Exception e) {
                logger.severe("Failed to stop client " + client.getClientId() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Handle a control change from hardware (forward path)
     * This is called by hardware clients when they receive input from physical controls
     */
    public void handleHardwareInput(String clientId, String canonicalId, int newValue) {
        IControlClient client = clients.get(clientId);
        if (client == null) {
            logger.warning("Received input from unknown client: " + clientId);
            return;
        }
        
        // Set thread-local origin to prevent echo back to this client
        ThreadLocal<Object> origin = clientOrigins.get(clientId);
        if (origin != null) {
            origin.set(clientId);
        }
        
        try {
            // Update the canonical control instance
            ControlInstance ci = registry.resolveCanonicalId(canonicalId);
            if (ci != null) {
                ci.updateValue(newValue);
            } else {
                logger.warning("Unknown canonical ID from hardware: " + canonicalId);
            }
        } finally {
            // Clear the origin
            if (origin != null) {
                origin.set(null);
            }
        }
    }
    
    /**
     * Handle a bank change from hardware
     */
    public void handleHardwareBankChange(String clientId, IControlClient.BankType bankType, 
                                         int bankIndex, int stripIndex) {
        IControlClient client = clients.get(clientId);
        if (client != null) {
            client.handleBankChange(bankType, bankIndex, stripIndex);
        }
    }
    
    /**
     * Handle registry reload (when desk changes or config reloads)
     */
    public void onRegistryReload(CanonicalRegistry newRegistry) {
        // Detach from old registry instances
        detachFromRegistry();
        
        // Update to new registry (reference passed by caller)
        // Note: The manager itself doesn't store the registry reference
        // as it's passed in constructor and ControlListener will be called
        
        // Attach to new registry instances
        attachToRegistry();
        
        // Notify all clients of registry reload
        for (IControlClient client : clients.values()) {
            try {
                client.onRegistryReload(newRegistry);
            } catch (Exception e) {
                logger.severe("Client " + client.getClientId() + " failed to handle registry reload: " + e.getMessage());
            }
        }
        
        logger.info("Hardware clients notified of registry reload");
    }
    
    @Override
    public void onControlChanged(ControlInstance instance, int newValue) {
        // This is the ControlListener callback - called when any control instance changes
        String canonicalId = instance.getCanonicalId();
        
        // Dispatch to all clients, skipping those that originated the change
        for (Map.Entry<String, IControlClient> entry : clients.entrySet()) {
            String clientId = entry.getKey();
            IControlClient client = entry.getValue();
            
            // Check if this client originated the change
            ThreadLocal<Object> origin = clientOrigins.get(clientId);
            if (origin != null && clientId.equals(origin.get())) {
                if (debug) logger.fine("Skipping echo to origin client: " + clientId);
                continue;
            }
            
            // Send the change to the client
            try {
                if (client.isRunning()) {
                    client.handleControlChange(canonicalId, newValue, this);
                }
            } catch (Exception e) {
                logger.severe("Client " + clientId + " failed to handle control change: " + e.getMessage());
            }
        }
    }
    
    private void attachToRegistry() {
        for (ControlInstance ci : registry.getAllInstances()) {
            ci.addListener(this);
        }
        
        // Also add the registry reload listener
        for (ControlInstance ci : registry.getAllInstances()) {
            ci.addListener(registryReloadListener);
        }
        
        logger.fine("Attached hardware client manager to registry");
    }
    
    private void detachFromRegistry() {
        for (ControlInstance ci : registry.getAllInstances()) {
            ci.removeListener(this);
            ci.removeListener(registryReloadListener);
        }
        
        logger.fine("Detached hardware client manager from registry");
    }
    
    public static void enableDebug() {
        debug = true;
        logger.info("HardwareClientManager debug enabled");
    }
}