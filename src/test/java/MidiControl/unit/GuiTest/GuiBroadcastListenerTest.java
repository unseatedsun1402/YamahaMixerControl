package MidiControl.unit.GuiTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;
import MidiControl.Mocks.FakeSession;
import MidiControl.Server.SubscriptionManager;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;
import MidiControl.UserInterface.CanonicalContextResolver;
import MidiControl.UserInterface.Frontend.GuiBroadcastListener;
import MidiControl.UserInterface.Frontend.GuiBroadcaster;

public class GuiBroadcastListenerTest {

    @Test
    void testGuiBroadcastListenerWithRealControlInstance() throws Exception {

        // Arrange: build a real control instance from JSON
        String json =
        "[{"
            + "\"control_group\": \"kInputHA\","
            + "\"control_id\": 41,"
            + "\"sub_control\": \"kHAPhantom\","
            + "\"value\": 0,"
            + "\"min_value\": 0,"
            + "\"max_value\": 1,"
            + "\"max_channels\" : 56,"
            + "\"default_value\": 0,"
            + "\"comment\": \"Off, On\","
            + "\"key\": 266573250601,"
            + "\"parameter_change_format\": ["
            + "240, 67, \"1n\", 62, 17, 1, 0, 41, 0, 0, "
            + "\"cc\", \"cc\", "
            + "\"dd\", \"dd\", \"dd\", \"dd\", \"dd\", "
            + "247"
            + "],"
            + "\"parameter_request_format\": ["
            + "240, 67, \"3n\", 62, 17, 1, 0, 41, 0, 0, "
            + "\"cc\", \"cc\", "
            + "247"
            + "]"
            + "}]";

        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromString(json);

        CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));
        ControlInstance instance = registry
                .getGroups().get("kInputHA")
                .getSubcontrols().get("kHAPhantom")
                .getInstances().get(0);

        // Capture broadcast output
        StringBuilder captured = new StringBuilder();

        GuiBroadcaster fakeBroadcaster = (msg, ctx) -> captured.append(msg);

        CanonicalContextResolver fakeResolver = canonicalId -> "test.context";

        GuiBroadcastListener listener = new GuiBroadcastListener(fakeBroadcaster, fakeResolver);

        // Act
        listener.onControlChanged(instance, 77);

        // Assert
        assertEquals(
            "{\"type\":\"control-update\",\"payload\":{\"canonicalId\":\"kInputHA.kHAPhantom.0\",\"value\":77}}",
            captured.toString()
        );
    }

    @Test
    void testGuiBroadcastListenerRoutesByContext() {
        SubscriptionManager subs = new SubscriptionManager();

        FakeSession s1 = new FakeSession("A");
        FakeSession s2 = new FakeSession("B");

        subs.subscribe(s1,"channel.1");
        subs.subscribe( s2,"mix.3");

        StringBuilder captured = new StringBuilder();

        GuiBroadcaster broadcaster = (json, ctx) -> {
            for (var session : subs.getSubscribers(ctx)) {
                captured.append(session.getId()).append(":").append(json).append("\n");
            }
        };

        CanonicalContextResolver resolver = canonicalId -> "channel.1";

        GuiBroadcastListener listener = new GuiBroadcastListener(broadcaster, resolver);

        // Build a tiny real mapping so ControlInstance is valid
        String tinyJson =
        "[{"
        + "\"control_group\": \"TestGroup\","
        + "\"control_id\": 1,"
        + "\"sub_control\": \"TestSub\","
        + "\"value\": 0,"
        + "\"min_value\": 0,"
        + "\"max_value\": 127,"
        + "\"max_channels\": 1,"
        + "\"default_value\": 0,"
        + "\"comment\": \"\","
        + "\"key\": 12345,"
        + "\"parameter_change_format\": [240, 0, 0, 0, \"dd\", 247],"
        + "\"parameter_request_format\": [240, 0, 0, 0, 247]"
        + "}]";

        List<SysexMapping> tinyMappings = SysexMappingLoader.loadMappingsFromString(tinyJson);
        CanonicalRegistry tinyRegistry = new CanonicalRegistry(tinyMappings, new SysexParser(tinyMappings));

        ControlInstance fake = tinyRegistry
        .getGroups().get("TestGroup")
        .getSubcontrols().get("TestSub")
        .getInstances().get(0);

        listener.onControlChanged(fake, 55);

        String output = captured.toString();

        assertTrue(output.contains("A:"), "Subscriber A should receive the update");
        assertFalse(output.contains("B:"), "Subscriber B should NOT receive the update");
    }

}