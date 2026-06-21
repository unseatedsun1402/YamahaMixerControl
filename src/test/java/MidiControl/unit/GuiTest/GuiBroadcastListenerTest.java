package MidiControl.unit.GuiTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;
import MidiControl.Mocks.FakeSession;
import MidiControl.Mocks.FakeSysexMapping;
import MidiControl.Server.SubscriptionManager;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexParser;
import MidiControl.UserInterface.CanonicalContextResolver;
import MidiControl.UserInterface.Frontend.GuiBroadcastListener;
import MidiControl.UserInterface.Frontend.GuiBroadcaster;

public class GuiBroadcastListenerTest {

    @Test
    void testGuiBroadcastListenerWithRealControlInstance() throws Exception {

        List<SysexMapping> mappings = FakeSysexMapping.fakeSysexMapping();

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
            "{\"type\":\"control-update\",\"payload\":{\"canonicalId\":" +
                "\"kInputHA.kHAPhantom.0\",\"value\":77,\"min\":0,\"max\":1}}",
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

        List<SysexMapping> mappings = FakeSysexMapping.fakeSysexMapping();
        CanonicalRegistry tinyRegistry = new CanonicalRegistry(mappings, new SysexParser(mappings));

        ControlInstance fake = tinyRegistry.getGroup("kInputHA").getSubcontrol("kHAPhantom").getInstances().get(0);

        listener.onControlChanged(fake, 55);

        String output = captured.toString();

        assertTrue(output.contains("A:"), "Subscriber A should receive the update");
        assertFalse(output.contains("B:"), "Subscriber B should NOT receive the update");
    }

}