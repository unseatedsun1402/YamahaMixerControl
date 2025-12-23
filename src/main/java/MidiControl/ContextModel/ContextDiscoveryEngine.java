package MidiControl.ContextModel;

import java.util.*;
import java.util.logging.Logger;

import MidiControl.Controls.CanonicalRegistry;

public class ContextDiscoveryEngine {

    private static final Logger log = Logger.getLogger(ContextDiscoveryEngine.class.getName());

    private final CanonicalRegistry registry;
    private final List<ContextDiscoverer> discoverers;

    public ContextDiscoveryEngine(CanonicalRegistry registry) {
        this.registry = registry;

        // Register all discoverers here
        this.discoverers = List.of(
            new ChannelContextDiscoverer(),
            new MixContextDiscoverer(),
            new MatrixContextDiscoverer(),
            new OutputContextDiscoverer()
            // later: new ProcessingContextDiscoverer()
        );
    }

    public List<Context> discoverContexts() {
        log.info("=== Starting Context Discovery ===");

        List<Context> contexts = new ArrayList<>();

        // Run each discoverer in order
        for (ContextDiscoverer d : discoverers) {
            d.discover(contexts, registry);
        }

        log.info("=== Finished Context Discovery ===");
        log.info("Total contexts discovered: " + contexts.size());

        return contexts;
    }
}