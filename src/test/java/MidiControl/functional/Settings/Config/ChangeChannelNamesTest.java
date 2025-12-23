package MidiControl.functional.Settings.Config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import MidiControl.MidiDeviceManager.Configs.ChannelNameStore;
import MidiControl.MidiDeviceManager.Configs.ChannelNameStoreListener;
import MidiControl.Mocks.MockHttpServletRequest;
import MidiControl.Mocks.MockHttpServletResponse;
import MidiControl.Mocks.MockServletConfig;
import MidiControl.Mocks.MockServletContext;
import MidiControl.Services.ChangeJSONNames;
import MidiControl.TestUtilities.TestFileUtils;
import jakarta.servlet.ServletContextEvent;

public class ChangeChannelNamesTest {
    @Test
    public void testEndToEndChannelNameUpdate() throws Exception {
        // Create a temp directory
        File dir = Files.createTempDirectory("chan-test").toFile();

        // Create the EXACT file the initializer expects
        File tmp = new File(dir, "channels.json");
        TestFileUtils.writeFile(tmp, "[{\"short\":\"OldName\"}]");

        // Point config system to this directory
        System.setProperty("midicontrol.config.dir", dir.getAbsolutePath());

        MockServletContext ctx = new MockServletContext();
        new ChannelNameStoreListener().contextInitialized(new ServletContextEvent(ctx));

        ChangeJSONNames servlet = new ChangeJSONNames();
        servlet.init(new MockServletConfig(ctx));

        MockHttpServletRequest req = new MockHttpServletRequest(ctx);
        MockHttpServletResponse resp = new MockHttpServletResponse();

        req.setParameter("channel", "1");
        req.setParameter("name", "UpdatedName");

        servlet.doPost(req, resp);

        assertEquals("UpdatedName",
            ChannelNameStore.getAll().get(0).get("short").getAsString());

        String json = TestFileUtils.readFile(tmp);
        assertTrue(json.contains("UpdatedName"));
    }
}
