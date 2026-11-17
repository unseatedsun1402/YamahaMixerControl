package MidiControl.ContextModel;

import MidiControl.Controls.CanonicalRegistry;
import java.util.*;
import java.util.logging.Logger;

public class ContextDiscoveryEngine {

  private static final Logger log = Logger.getLogger(ContextDiscoveryEngine.class.getName());

  private final CanonicalRegistry registry;
  private List<ContextDiscoverer> discoverers = new ArrayList<>();

  public ContextDiscoveryEngine(CanonicalRegistry registry) {
    this.registry = registry;
    registerDiscoverers();
  }

  private void registerDiscoverers() {
    this.discoverers.add(new ChannelContextDiscoverer());
    this.discoverers.add(new NameContextDiscoverer());
    this.discoverers.add(new BusContextDiscoverer());
    this.discoverers.add(new OutputContextDiscoverer());
    this.discoverers.add(new MuteContextDiscoverer());
    this.discoverers.add(new MonitorContextDiscoverer());
  }

  public void addDiscoverer(ContextDiscoverer d) {
    this.discoverers.add(d);
  }

  public List<Context> discoverContexts() {
    log.info("=== Starting Context Discovery ===");

    List<Context> contexts = new ArrayList<>();

    for (ContextDiscoverer d : discoverers) {
      d.discover(contexts, registry);
    }

    log.info("=== Finished Context Discovery ===");
    log.info("Total contexts discovered: " + contexts.size());

    return contexts;
  }

  public boolean matchesFilters(Context ctx, BankContext bankCtx) {

    for (BankFilter filter : bankCtx.getFilters()) {

      if (filter.getPrefix() != null) {
        if (!ctx.getId().startsWith(filter.getPrefix() + ".")) {
          continue; // prefix mismatch → try next filter
        }
      }

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

      if (filter.getType() != null) {
        if (ctx.getContextType() != filter.getType()) {
          continue;
        }
      }

      return true;
    }

    // No filters matched
    return false;
  }
}
