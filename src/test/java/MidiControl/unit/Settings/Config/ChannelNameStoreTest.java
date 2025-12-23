package MidiControl.unit.Settings.Config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import MidiControl.TestUtilities.TestFileUtils;

import MidiControl.MidiDeviceManager.Configs.ChannelNameStore;
import MidiControl.MidiDeviceManager.Configs.ChannelNameStoreListener;
import MidiControl.Mocks.MockServletContext;
import jakarta.servlet.ServletContextEvent;

public class ChannelNameStoreTest {
    @Test
    public void testInitializeLoadsJson() throws Exception {
        File tmp = TestFileUtils.createTempJson("[{\"short\":\"Ch1\"},{\"short\":\"Ch2\"}]");

        ChannelNameStore.initialize(tmp);

        var all = ChannelNameStore.getAll();
        assertEquals(2, all.size());
        assertEquals("Ch1", all.get(0).get("short").getAsString());
    }

    @Test
    public void testListenerFailsOnMissingFile() throws Exception {
        // Create a file where a directory is expected
        File notADir = File.createTempFile("not-a-dir", ".txt");

        System.setProperty("midicontrol.config.dir", notADir.getAbsolutePath());

        MockServletContext ctx = new MockServletContext();
        ChannelNameStoreListener listener = new ChannelNameStoreListener();

        assertThrows(RuntimeException.class, () ->
            listener.contextInitialized(new ServletContextEvent(ctx))
        );
    }

    @Test
    public void testSetNameWritesToDisk() throws IOException {
        File tmp = TestFileUtils.createTempJson("[{\"short\":\"Old\"}]");

        ChannelNameStore.initialize(tmp);
        ChannelNameStore.setName(1, "Updated");

        String json = Files.readString(tmp.toPath());
        assertTrue(json.contains("Updated"));
    }

    

}
