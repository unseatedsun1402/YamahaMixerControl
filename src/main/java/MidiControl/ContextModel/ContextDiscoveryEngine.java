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
            new BusContextDiscoverer()
            // new MatrixContextDiscoverer(),
            // new OutputContextDiscoverer()
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

    public boolean matchesFilters(Context ctx, BankContext bankCtx) {

        for (BankFilter filter : bankCtx.getFilters()) {

            // 1. Match prefix (e.g. "channel" from "channel.5")
            if (filter.getPrefix() != null) {
                if (!ctx.getId().startsWith(filter.getPrefix() + ".")) {
                    continue; // prefix mismatch → try next filter
                }
            }

            // 2. Match index (if specified)
            if (filter.getIndex() != null) {
                String[] parts = ctx.getId().split("\\.");
                if (parts.length != 2) continue;

                try {
                    int ctxIndex = Integer.parseInt(parts[1]);
                    if (ctxIndex != filter.getIndex()) {
                        continue;
                    }
                } catch (NumberFormatException ex) {
                    continue;
                }
            }

            // 3. Match context type
            if (filter.getType() != null) {
                if (ctx.getContextType() != filter.getType()) {
                    continue;
                }
            }

            // If we reach here → this filter matches
            return true;
        }

        // No filters matched
        return false;
    }
}