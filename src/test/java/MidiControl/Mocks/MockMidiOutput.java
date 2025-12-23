package MidiControl.Mocks;

import MidiControl.MidiDeviceManager.MidiOutput;
import java.util.ArrayList;
import java.util.List;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;

public class MockMidiOutput implements MidiOutput {
  private final List<MidiMessage> sentMessages = new ArrayList<>();

  @Override
  public void sendMessage(MidiMessage message) {
    sentMessages.add((MidiMessage) message);
  }

  public List<MidiMessage> getSentMessages() {
    return sentMessages;
  }

  @Override
  public Info getDeviceInfo() {
    return MidiSystem.getMidiDeviceInfo()[0]; // 0 should be the system internal midi interface
  }

  @Override
  public boolean isOpen() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'isOpen'");
  }

  @Override
  public void close() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'close'");
  }
}
