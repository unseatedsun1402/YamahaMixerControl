package MidiControl.unit.Server;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;

import MidiControl.Mocks.MockServletContext;
import MidiControl.Server.MidiServer;
import MidiControl.Server.MidiServerListener;
import jakarta.servlet.ServletContextEvent;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

public class MidiServerListenerTest {

  @Test
  public void testServerIsCreatedAndStoredInContext() {
    try (MockedConstruction<MidiServer> servers = mockConstruction(MidiServer.class)) {
      MockServletContext ctx = new MockServletContext();
      MidiServerListener listener = new MidiServerListener();
      ServletContextEvent event = new ServletContextEvent(ctx);

      listener.contextInitialized(event);

      assertEquals(1, servers.constructed().size());
      MidiServer server = servers.constructed().get(0);
      assertSame(server, ctx.getAttribute("midiServer"));
      verify(server).run();

      listener.contextDestroyed(event);
      verify(server).shutdown();
    }
  }

  @Test
  public void testServerShutdownIsCalled() {
    MockServletContext ctx = new MockServletContext();
    MidiServerListener listener = new MidiServerListener();

    MidiServer fake = mock(MidiServer.class);
    ctx.setAttribute("midiServer", fake);

    listener.contextDestroyed(new ServletContextEvent(ctx));

    verify(fake).shutdown();
  }
}
