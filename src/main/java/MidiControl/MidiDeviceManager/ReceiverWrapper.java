package MidiControl.MidiDeviceManager;

import MidiControl.ControlServer.InputHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;

public class ReceiverWrapper implements MidiOutput {
  private final MidiDevice device;
  private Receiver receiver;

  public ReceiverWrapper(MidiDevice device) throws MidiUnavailableException {
    this.device = device;
    setup();
  }

  private void setup() throws MidiUnavailableException {
    Logger logger = Logger.getLogger(InputHandler.class.getName());
    if (!device.isOpen()) {
      device.open();
      logger.log(Level.INFO, "Opened MIDI device for receiver." + device.getDeviceInfo().getName());
    }

    if (device.getMaxReceivers() != 0) {
      logger.info("Max receivers: " + device.getMaxReceivers());
      receiver = device.getReceiver();
    } else {
      throw new MidiUnavailableException("Device has no receiver available.");
    }
    logger.log(Level.INFO, "Receiver initialized and ready.");
  }

  @Override
  public void sendMessage(MidiMessage message) {
    Logger logger = Logger.getLogger(InputHandler.class.getName());
    if (receiver != null) {
      receiver.send(message, -1); // -1 = immediate
      logger.log(
          Level.FINE,
          "Sent MIDI: cmd={0}, data1={1}, data2={2}",
          new Object[] {message.getMessage()[0], message.getMessage()[1], message.getMessage()[2]});
    } else {
      logger.log(Level.WARNING, "Receiver is null. Cannot send MIDI message.");
    }
  }

  public Receiver getRawReceiver() {
    return receiver;
  }

  public void close() {
    Logger logger = Logger.getLogger(InputHandler.class.getName());
    if (receiver != null) {
      receiver.close();
    }
    if (device.isOpen()) {
      device.close();
    }
    logger.log(Level.INFO, "Closed receiver and MIDI device.");
  }

  public boolean isOpen() {
    return device.isOpen();
  }

  @Override
  public MidiDevice.Info getDeviceInfo() {
    return this.device.getDeviceInfo();
  }
}
