package MidiControl.unit;

import MidiControl.Server.ServerShutdown;
import MidiControl.Services.LocalServiceDiscovery;

import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ServerShutdownTest {

    @Test
    void closesDiscoveryAndApplicationContextOnce() throws Exception {
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        LocalServiceDiscovery discovery = mock(LocalServiceDiscovery.class);
        ServerShutdown shutdown = new ServerShutdown(context, discovery);

        assertNull(shutdown.shutdown());
        assertNull(shutdown.shutdown());

        verify(discovery).close();
        verify(context).close();
    }

    @Test
    void closesApplicationContextEvenWhenDiscoveryCloseFails() throws Exception {
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        LocalServiceDiscovery discovery = mock(LocalServiceDiscovery.class);
        IOException failure = new IOException("Could not stop discovery");
        doThrow(failure).when(discovery).close();
        ServerShutdown shutdown = new ServerShutdown(context, discovery);

        assertSame(failure, shutdown.shutdown());

        verify(context, times(1)).close();
    }
}
