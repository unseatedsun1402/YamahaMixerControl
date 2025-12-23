package MidiControl.TestUtilities;

import MidiControl.NrpnUtils.NrpnParser;
import MidiControl.ControlServer.CanonicalInputEvent;
import MidiControl.ControlServer.HardwareInputHandler;

import javax.sound.midi.ShortMessage;

public class NrpnMocker {

    private final NrpnParser parser;
    private final HardwareInputHandler inputHandler;

    public NrpnMocker(NrpnParser parser, HardwareInputHandler inputHandler) {
        this.parser = parser;
        this.inputHandler = inputHandler;
    }

  public CanonicalInputEvent sendNrpn(int msb, int lsb, int value) throws Exception {

      int valueMsb = (value >> 7) & 0x7F;
      int valueLsb = value & 0x7F;

      // NRPN MSB
      inputHandler.handle(new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 99, msb));

      // NRPN LSB
      inputHandler.handle(new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 98, lsb));

      // Data Entry MSB
      inputHandler.handle(new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 6, valueMsb));

      // Data Entry LSB → this produces the CanonicalInputEvent
      return inputHandler.handle(
          new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 38, valueLsb)
      );
  }
}