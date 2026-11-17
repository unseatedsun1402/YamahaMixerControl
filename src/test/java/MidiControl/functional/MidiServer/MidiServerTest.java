package MidiControl.functional.MidiServer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.DeskDiscovery.DeskDiscovery;
import MidiControl.LivenessMonitor.DeviceLivenessMonitor;
import MidiControl.MidiDeviceManager.MidiIOManager;
import MidiControl.MidiDeviceManager.MidiInputReceiver;
import MidiControl.MidiDeviceManager.TransportMode;
import MidiControl.Mocks.FakeSession;
import MidiControl.Mocks.MockMidiOut;
import MidiControl.Server.MidiProcessingLoop;
import MidiControl.Server.MidiServer;
import MidiControl.TestUtilities.MidiTestUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.ShortMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

@Tag("functional")
public class MidiServerTest {
  private final List<MidiServer> servers = new ArrayList<>();

  private MidiServer createServer() {
    MidiServer server = new MidiServer();
    servers.add(server);
    return server;
  }

  @AfterEach
  void shutdownServers() {
    servers.forEach(MidiServer::shutdown);
    servers.clear();
  }

  @Test
  void testInputReceiverReceivesMessage() throws Exception {
    ConcurrentLinkedQueue<MidiMessage> inputBuffer = new ConcurrentLinkedQueue<>();

    try (MidiInputReceiver receiver = new MidiInputReceiver(inputBuffer)) {
      ShortMessage msg = new ShortMessage();
      msg.setMessage(ShortMessage.NOTE_ON, 0, 60, 127);

      receiver.send(msg, -1);

      assertFalse(
          inputBuffer.isEmpty(), "Input buffer should not be empty after receiving a message");
      assertEquals(1, inputBuffer.size(), "Input buffer should contain exactly one message");
      assertEquals(msg, inputBuffer.poll(), "Received message should match sent message");
    }
  }

  @Test
  void testSendMidiMessagesDispatchesToOutput() {
    MidiServer server = createServer();

    MockMidiOut mockOutput = new MockMidiOut();
    server.getMidiDeviceManager().setMidiOutForTest(mockOutput);

    byte[] msg1 = new byte[] {(byte) 0xB0, 7, 100}; // CC7
    byte[] msg2 = new byte[] {(byte) 0x90, 60, 127}; // Note On

    server.getMidiDeviceManager().getMidiOut().sendMessage(msg1);
    server.getMidiDeviceManager().getMidiOut().sendMessage(msg2);

    List<byte[]> sent = mockOutput.getSentMessages();

    assertEquals(2, sent.size(), "Expected 2 MIDI messages to be sent");
    assertArrayEquals(msg1, sent.get(0));
    assertArrayEquals(msg2, sent.get(1));
  }

  @Test
  void testIncomingMidiParsingDoesNotThrow() throws InvalidMidiDataException {
    try (MockedConstruction<MidiIOManager> io = mockConstruction(MidiIOManager.class);
        MockedConstruction<DeskDiscovery> discovery = mockConstruction(DeskDiscovery.class)) {
      ShortMessage msg = new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 99, 1);
      MidiServer server = createServer();
      assertEquals(1, io.constructed().size());
      server.addtoinputqueue(msg);
      assertDoesNotThrow(server::processIncomingMidiForTest);
      assertEquals(0, server.getInputBufferSize());
      assertTrue(discovery.constructed().isEmpty(), "MIDI parsing must not start desk discovery");
    }
  }

  @Test
  void testSetInputDeviceWithOutputOnlyIndexFailsGracefully() throws Exception {
    MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
    assertTrue(infos.length > 0, "Expected at least one MIDI device for this test");

    MidiDevice device = MidiSystem.getMidiDevice(infos[0]);

    if (device.getMaxTransmitters() == 0) {
      MidiServer server = createServer();
      boolean wasOpen = device.isOpen();

      assertDoesNotThrow(
          () -> server.getMidiDeviceManager().trySetInputDevice(0),
          "Should throw for invalid device index");
      if (!wasOpen) {
        assertFalse(
            device.isOpen(), "An output-only device must be closed after input setup fails");
      }
    }
  }

  @Test
  void testProcessIncomingMidiHandlesEmptyBuffer() {
    try (MockedConstruction<MidiIOManager> io = mockConstruction(MidiIOManager.class);
        MockedConstruction<DeskDiscovery> discovery = mockConstruction(DeskDiscovery.class)) {
      MidiServer server = createServer();
      assertEquals(1, io.constructed().size());
      assertDoesNotThrow(server::processIncomingMidiForTest);
      assertEquals(0, server.getInputBufferSize());
      assertTrue(discovery.constructed().isEmpty(), "MIDI parsing must not start desk discovery");
    }
  }

  @Test
  void discoveredRegistryAndProfileReloadKeepBankModelsInSyncWithClientChanges() {
    try (MockedConstruction<MidiIOManager> io = mockConstruction(MidiIOManager.class,
            (mock, context) -> {
              when(mock.getTransportMode()).thenReturn(TransportMode.NRPN);
              when(mock.trySetInputDevice(anyInt())).thenReturn(true);
              when(mock.trySetOutputDevice(anyInt())).thenReturn(true);
            });
        MockedConstruction<DeviceLivenessMonitor> liveness = mockConstruction(DeviceLivenessMonitor.class);
        MockedConstruction<MidiProcessingLoop> processing = mockConstruction(MidiProcessingLoop.class)) {
      MidiServer server = createServer();
      CanonicalRegistry initial = server.getCanonicalRegistry();
      server.run();
      CanonicalRegistry discovered = server.getCanonicalRegistry();
      assertNotSame(initial, discovered);
      FakeSession session = new FakeSession("bank-state");

      //asserts satisfy lint referenced but not used
      assertFalse(liveness.constructed().isEmpty());
      assertFalse(processing.constructed().isEmpty());

      requestModel(server, session, "channel.0");
      changeFader(server, session, 77);
      requestModel(server, session, "channel.8");
      assertEquals(77, faderValue(requestModel(server, session, "channel.0")));
      assertEquals(77, discovered.resolveCanonicalId("kInputFader.kFader.0").getValue());
      assertNotEquals(77, initial.resolveCanonicalId("kInputFader.kFader.0").getValue());

      ArgumentCaptor<byte[]> messages = ArgumentCaptor.forClass(byte[].class);
      verify(io.constructed().get(0), atLeastOnce()).sendAsync(messages.capture());
      List<byte[]> expected = discovered.resolveCanonicalId("kInputFader.kFader.0").buildNrpnChange(77);
      assertEquals(expected.size(), messages.getAllValues().size());
      for (int i = 0; i < expected.size(); i++) {
        assertArrayEquals(expected.get(i), messages.getAllValues().get(i));
      }

      clearInvocations(io.constructed().get(0));
      server.getServerRouter().handleMessage(session, """
          {"type":"apply-midi-settings","payload":{"inputDeviceId":0,"outputDeviceId":0,
          "consoleType":"YAMAHA_M7CL","safeprofile":true,"mainprofile":false,"highprofile":false}}
          """);
      assertNotNull(server.getCanonicalRegistry().getGroup("kMixToMatrix"));
      changeFader(server, session, 95);
      requestModel(server, session, "channel.8");
      assertEquals(95, faderValue(requestModel(server, session, "channel.0")));
      messages = ArgumentCaptor.forClass(byte[].class);
      verify(io.constructed().get(0), atLeastOnce()).sendAsync(messages.capture());
      expected = server.getCanonicalRegistry().resolveCanonicalId("kInputFader.kFader.0")
          .buildNrpnChange(95);
      assertEquals(expected.size(), messages.getAllValues().size());
      for (int i = 0; i < expected.size(); i++) {
        assertArrayEquals(expected.get(i), messages.getAllValues().get(i));
      }
    }
  }

  private static void changeFader(MidiServer server, FakeSession session, int value) {
    server.getServerRouter().handleMessage(session, """
        {"type":"set-control-value","payload":{"canonicalId":"kInputFader.kFader.0","value":%d}}
        """.formatted(value));
  }

  private static JsonObject requestModel(MidiServer server, FakeSession session, String context) {
    server.getServerRouter().handleMessage(session, """
        {"type":"get-ui-model","payload":{"contextId":"%s","uiType":"basic-input-view"}}
        """.formatted(context));
    return JsonParser.parseString(session.lastSent).getAsJsonObject().getAsJsonObject("payload");
  }

  private static int faderValue(JsonObject model) {
    for (var element : model.getAsJsonArray("controls")) {
      JsonObject control = element.getAsJsonObject();
      if ("kInputFader.kFader.0".equals(control.get("canonicalId").getAsString())) {
        return control.get("value").getAsInt();
      }
    }
    throw new AssertionError("Fader not found in bank model");
  }
  @Test
  void testInputQueueHelperAddsMessages() throws Exception {
    ShortMessage msg = MidiTestUtils.createControlChange(0, 99, 1);
    MidiServer server = createServer();
    int before = server.getInputBufferSize();
    server.addtoinputqueue(msg);
    int after = server.getInputBufferSize();
    assertEquals(before + 1, after, "Input buffer should grow by 1 after adding a message");
  }
}
