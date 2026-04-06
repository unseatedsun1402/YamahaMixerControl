package MidiControl.MidiDeviceManager;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.*;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;

public class InputWrapper implements MidiInput {
  private final MidiDevice device;
  private Transmitter transmitter;
  private MidiInputReceiver inputReceiver;
  private final ConcurrentLinkedQueue<MidiMessage> inputBuffer;

  public InputWrapper(MidiDevice device, ConcurrentLinkedQueue<MidiMessage> inputBuffer)
      throws MidiUnavailableException {
    this.device = device;
    this.inputBuffer = inputBuffer;
    setup();
  }

  private void setup() throws MidiUnavailableException {
    if (!device.isOpen()) {
      device.open();
      Logger.getLogger(InputWrapper.class.getName())
          .log(Level.INFO, "Opened MIDI device for transmitter: "+device.getDeviceInfo().getName());
    }

    transmitter = device.getTransmitter();

    if (inputReceiver != null) {
      inputReceiver.close();
      Logger.getLogger(InputWrapper.class.getName())
          .log(Level.INFO, "Restarting existing MidiInputReceiver.");
    }

    inputReceiver = new MidiInputReceiver(inputBuffer);
    transmitter.setReceiver(inputReceiver);
    Logger.getLogger(InputWrapper.class.getName())
        .log(Level.INFO, "Transmitter set and inputReceiver attached.");
  }

  @Override
  public void setReceiver(Receiver receiver) {
    transmitter.setReceiver(receiver);
  }

  public boolean isOpen() {
    return device.isOpen();
  }

  @Override
  public void close() {
    if (transmitter != null) {
      transmitter.close();
    }
    if (inputReceiver != null) {
      inputReceiver.close();
    }
    if (device.isOpen()) {
      device.close();
    }
    Logger.getLogger(InputWrapper.class.getName())
        .log(Level.INFO, "Closed transmitter and device.");
  }

  public Transmitter getRawTransmitter() {
    return this.transmitter;
  }

  @Override
  public MidiDevice.Info getDeviceInfo() {
    return this.device.getDeviceInfo();
  }

  @Override
  public MidiInputReceiver getInputReceiver() {
      return (MidiInputReceiver) inputReceiver;
  }
}
