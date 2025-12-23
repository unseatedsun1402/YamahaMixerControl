package MidiControl.functional;

import static org.junit.jupiter.api.Assertions.assertFalse;

import MidiControl.MidiServer;
import MidiControl.NrpnUtils.NrpnParser;
import MidiControl.NrpnUtils.NrpnRegistry;
import MidiControl.Socket;
import java.util.ArrayList;
import java.util.List;
import javax.sound.midi.ShortMessage;
import org.junit.jupiter.api.Test;

public class NrpnTestRunner {
  // public static void main(String[] args) throws Exception {
  //     NrpnParser parser = new NrpnParser();
  //     MidiControl.TestUtilities.NrpnMocker mocker = new
  // MidiControl.TestUtilities.NrpnMocker(parser);

  //     // Optional: set a dispatch target
  //     NrpnParser.setDispatchTarget(new NRPNDispatchTarget() {
  //         public void handleResolvedNRPN(NrpnMapping mapping, int value) {
  //             System.out.println("Dispatched: " + mapping.label() + " → " + value);
  //         }
  //     });

  //     // Load registry
  //     NrpnRegistry.getInstance().loadFromJson("nrpn_mappings.json");

  //     // Send a known NRPN
  //     mocker.sendNrpn(0x01, 0x00, 8192); // Mix 9 Send 3
  // }

  @Test
  public void testNrpnPipelineEndToEnd() throws Exception {
    MidiServer server = new MidiServer();

    List<String> broadcasts = new ArrayList<>();
    Socket.testBroadcastHook = broadcasts::add;

    NrpnRegistry.getInstance().loadFromClasspath("MidiControl/nrpn_mappings.json");
    NrpnParser.setDispatchTarget(server);

    ShortMessage msb = new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 99, 1);
    ShortMessage lsb = new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 98, 0);
    ShortMessage valMsb = new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 6, 96);
    ShortMessage valLsb = new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 38, 57);

    MidiServer.addtoinputqueue(msb);
    MidiServer.addtoinputqueue(lsb);
    MidiServer.addtoinputqueue(valMsb);
    MidiServer.addtoinputqueue(valLsb);

    server.processIncomingMidi();
    assertFalse(broadcasts.isEmpty(), "Expected at least one broadcast message");
    System.out.println("Broadcast JSON: " + broadcasts.get(0));
  }
}
