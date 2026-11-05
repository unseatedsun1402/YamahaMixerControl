package MidiControl.Server;

import org.springframework.context.ConfigurableApplicationContext;

import MidiControl.Services.LocalServiceDiscovery;

import java.io.IOException;

public class ServerShutdown {
    private final ConfigurableApplicationContext context;
    private final LocalServiceDiscovery discovery;
    private boolean shutDown;

    public ServerShutdown(ConfigurableApplicationContext context, LocalServiceDiscovery discovery) {
        this.context = context;
        this.discovery = discovery;
    }

    public synchronized IOException shutdown() {
        if (shutDown) {
            return null;
        }
        shutDown = true;

        IOException discoveryFailure = null;
        if (discovery != null) {
            try {
                discovery.close();
            } catch (IOException exception) {
                discoveryFailure = exception;
            }
        }

        context.close();
        return discoveryFailure;
    }
}
