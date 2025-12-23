package MidiControl.functional.MidiServer;

import MidiControl.BuildPage;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Server.MidiServer;
import MidiControl.Server.MidiServerListener;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;
import MidiControl.Mocks.*;
import jakarta.servlet.ServletContextEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class MidiServerIntegrationTest {

    @Test
    public void testServerLifecycleAndServletAccess() throws Exception {
        // --- Minimal valid mapping ---
        String json =
            "[{"
                + "\"control_group\": \"kInput\","
                + "\"control_id\": 1,"
                + "\"sub_control\": \"Fader\","
                + "\"max_channels\": 8,"
                + "\"value\": 0,"
                + "\"min_value\": 0,"
                + "\"max_value\": 127,"
                + "\"default_value\": 0,"
                + "\"comment\": \"Test fader\","
                + "\"key\": 12345,"
                + "\"parameter_change_format\": [240, 0, 0, \"dd\", 247],"
                + "\"parameter_request_format\": [240, 0, 0, \"cc\", 247]"
            + "}]";

        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromString(json);
        CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));

        MockServletContext ctx = new MockServletContext();

        // Start the real server via listener
        MidiServerListener listener = new MidiServerListener();
        listener.contextInitialized(new ServletContextEvent(ctx));

        // Retrieve the real server instance
        MidiServer ms = (MidiServer) ctx.getAttribute("midiServer");
        assertNotNull(ms, "Server should have been initialized by listener");

        // Inject the registry into the real server
        ms.setCanonicalRegistry(registry);

        // Now servlet should see the server
        BuildPage servlet = new BuildPage();
        servlet.init(new MockServletConfig(ctx));

        MockHttpServletRequest req = new MockHttpServletRequest(ctx);
        MockHttpServletResponse resp = new MockHttpServletResponse();

        servlet.doGet(req, resp);

        assertEquals(200, resp.getStatus());
        System.out.println("Output content: " + resp.getContentAsString());
    }
}