package MidiControl.unit.GuiTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import MidiControl.ContextModel.Context;
import MidiControl.ContextModel.ContextFilter;
import MidiControl.ContextModel.ContextType;
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
import MidiControl.UserInterface.Frontend.WebSocketGuiBroadcaster;
import MidiControl.UserInterface.UiContextIndex;
import java.util.List;
import org.junit.jupiter.api.Test;

public class GuiBroadcastListenerTest {

  @Test
  void testGuiBroadcastListenerWithRealControlInstance() throws Exception {

    List<SysexMapping> mappings = FakeSysexMapping.fakeSysexMapping();

    CanonicalRegistry registry = new CanonicalRegistry(mappings, new SysexParser(mappings));
    ControlInstance instance =
        registry
            .getGroups()
            .get("kInputHA")
            .getSubcontrols()
            .get("kHAPhantom")
            .getInstances()
            .get(0);

    // Capture broadcast output
    StringBuilder captured = new StringBuilder();

    GuiBroadcaster fakeBroadcaster = (msg, ctx) -> captured.append(msg);

    CanonicalContextResolver fakeResolver = canonicalId -> "test.context";

    GuiBroadcastListener listener = new GuiBroadcastListener(fakeBroadcaster, fakeResolver);

    // Act
    listener.onControlChanged(instance, 77);

    // Assert
    assertEquals(
        "{\"type\":\"control-update\",\"payload\":{\"canonicalId\":"
            + "\"kInputHA.kHAPhantom.0\",\"value\":77,\"min\":0,\"max\":1}}",
        captured.toString());
  }

  @Test
  void testGuiBroadcastListenerRoutesByContext() {
    SubscriptionManager subs = new SubscriptionManager();

    FakeSession s1 = new FakeSession("A");
    FakeSession s2 = new FakeSession("B");

    subs.subscribe(s1, "channel.1");
    subs.subscribe(s2, "mix.3");

    StringBuilder captured = new StringBuilder();

    GuiBroadcaster broadcaster =
        (json, ctx) -> {
          for (var session : subs.getSubscribers(ctx)) {
            captured.append(session.getId()).append(":").append(json).append("\n");
          }
        };

    CanonicalContextResolver resolver = canonicalId -> "channel.1";

    GuiBroadcastListener listener = new GuiBroadcastListener(broadcaster, resolver);

    List<SysexMapping> mappings = FakeSysexMapping.fakeSysexMapping();
    CanonicalRegistry tinyRegistry = new CanonicalRegistry(mappings, new SysexParser(mappings));

    ControlInstance fake =
        tinyRegistry.getGroup("kInputHA").getSubcontrol("kHAPhantom").getInstances().get(0);

    listener.onControlChanged(fake, 55);

    String output = captured.toString();

    assertTrue(output.contains("A:"), "Subscriber A should receive the update");
    assertFalse(output.contains("B:"), "Subscriber B should NOT receive the update");
  }

  @Test
  void sharedControlsBroadcastOncePerSubscribedSession() {
    SubscriptionManager subs = new SubscriptionManager();
    FakeSession both = new FakeSession("both");
    FakeSession second = new FakeSession("second");
    subs.subscribe(both, "channel.0");
    subs.subscribe(both, "channel.1");
    subs.subscribe(second, "channel.1");

    UiContextIndex index = new UiContextIndex();
    ContextFilter shared = new ContextFilter("kInputHA", "kHAPhantom", 0);
    index.addAll(
        List.of(
            new Context("channel.0", "One", ContextType.CHANNEL, List.of(), List.of(shared)),
            new Context("channel.1", "Two", ContextType.CHANNEL, List.of(), List.of(shared))));
    List<SysexMapping> mappings = FakeSysexMapping.fakeSysexMapping();
    ControlInstance instance =
        new CanonicalRegistry(mappings, null)
            .getGroup("kInputHA")
            .getSubcontrol("kHAPhantom")
            .getInstances()
            .get(0);

    new GuiBroadcastListener(new WebSocketGuiBroadcaster(subs), index)
        .onControlChanged(instance, 1);

    assertEquals(1, both.getSentMessages().size());
    assertEquals(1, second.getSentMessages().size());
  }
}
