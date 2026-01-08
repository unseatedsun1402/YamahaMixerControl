package MidiControl.unit.BuildPage;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import java.util.List;

import MidiControl.BuildPage;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Mocks.MockHttpServletRequest;
import MidiControl.Mocks.MockHttpServletResponse;
import MidiControl.Mocks.MockServletConfig;
import MidiControl.Mocks.MockServletContext;
import MidiControl.Server.MidiServer;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;

public class BuildPageTest {
    @Test
    void testBuildPageOutputsCanonicalIds() throws Exception {

        // --- Minimal valid mapping ---
        String json =
            "[{"
                + "\"control_group\": \"kInput\","
                + "\"control_id\": 1,"
                + "\"sub_control\": \"Fader\","
                + "\"max_channels\": 9,"
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

        // --- Real MidiServer with overridden registry ---
        MidiServer server = new MidiServer() {
            @Override
            public CanonicalRegistry getCanonicalRegistry() {
                return registry;
            }
        };

        // --- Mock servlet environment ---
        MockServletContext ctx = new MockServletContext();
        ctx.setAttribute("midiServer", server);

        MockServletConfig config = new MockServletConfig(ctx);
        MockHttpServletRequest req = new MockHttpServletRequest(ctx);
        MockHttpServletResponse resp = new MockHttpServletResponse();

        BuildPage servlet = new BuildPage();
        servlet.init(config);

        // --- Act ---
        servlet.doGet(req, resp);

        String html = resp.getContentAsString();

        // --- Assert ---
        assertTrue(html.contains("data-canonical-id=\"kInput.Fader.0\""));
    }

    @Test
    void testBuildPageOutputsCanonicalIdsViaPost() throws Exception {

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

        MidiServer server = new MidiServer() {
            @Override
            public CanonicalRegistry getCanonicalRegistry() {
                return registry;
            }
        };

        MockServletContext ctx = new MockServletContext();
        ctx.setAttribute("midiServer", server);

        MockServletConfig config = new MockServletConfig(ctx);
        MockHttpServletRequest req = new MockHttpServletRequest(ctx);
        MockHttpServletResponse resp = new MockHttpServletResponse();

        BuildPage servlet = new BuildPage();
        servlet.init(config);

        // --- Act ---
        servlet.doPost(req, resp);

        String html = resp.getContentAsString();

        // --- Assert ---
        assertTrue(html.contains("data-canonical-id=\"kInput.Fader.0\""));
    }
}