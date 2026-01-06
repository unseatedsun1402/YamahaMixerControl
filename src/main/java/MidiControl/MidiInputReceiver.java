package MidiControl;

import MidiControl.SysexUtils.SysexParser;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;

/** Receives incoming MIDI messages and puts them into a buffer for processing. */
public class MidiInputReceiver implements Receiver {
  private final ConcurrentLinkedQueue<MidiMessage> inputBuffer;
  private volatile boolean open = true;
  private Logger logger = Logger.getLogger(getClass().getName());

  public MidiInputReceiver(ConcurrentLinkedQueue<MidiMessage> inputBuffer) {
    this.inputBuffer = inputBuffer;
  }

  @Override
  public void close() {
    open = false;
  }

  @Override
  public void send(MidiMessage message, long timeStamp) {
    if (message == null) {
      logger.warning("MidiMessage is null");
      return;
    }
    if (open) inputBuffer.add(message);
    logger.fine("Added to inputBuffer: " + SysexParser.bytesToHex(message.getMessage()));
  }

  public Boolean isOpen() {
    return this.open;
  }
}
