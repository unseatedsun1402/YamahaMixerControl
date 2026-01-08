package MidiControl.unit.Server;

import MidiControl.Server.MidiServer;
import MidiControl.Server.MidiServerListener;
import MidiControl.Mocks.MockServletContext;
import jakarta.servlet.ServletContextEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MidiServerListenerTest {

    @Test
    public void testServerIsCreatedAndStoredInContext() {
        MockServletContext ctx = new MockServletContext();
        MidiServerListener listener = new MidiServerListener();

        listener.contextInitialized(new ServletContextEvent(ctx));

        Object server = ctx.getAttribute("midiServer");
        assertNotNull(server);
        assertTrue(server instanceof MidiServer);
    }

    @Test
    public void testServerShutdownIsCalled() {
        MockServletContext ctx = new MockServletContext();
        MidiServerListener listener = new MidiServerListener();

        // Insert a fake server so we can detect shutdown
        FakeMidiServer fake = new FakeMidiServer();
        ctx.setAttribute("midiServer", fake);

        listener.contextDestroyed(new ServletContextEvent(ctx));

        assertTrue(fake.shutdownCalled);
    }

    // Simple fake server for shutdown verification
    private static class FakeMidiServer extends MidiServer {
        boolean shutdownCalled = false;

        @Override
        public void shutdown() {
            shutdownCalled = true;
        }
    }
}