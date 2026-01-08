package MidiControl.MidiDeviceManager;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;

public final class MidiDeviceUtils {

  private MidiDeviceUtils() {} // prevent instantiation
  public static MidiDevice.Info[] overrideDeviceInfos = null;
  public static MidiDevice overrideDevice = null;

  public static MidiDevice getDevice(int index) throws MidiUnavailableException {
    MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
    if (index < 0 || index >= infos.length) {
      throw new MidiUnavailableException("Invalid device index: " + index);
    }
    return MidiSystem.getMidiDevice(infos[index]);
  }

  public static int getDeviceIndex(MidiDevice.Info targetInfo) {
    MidiDevice.Info[] allInfos = MidiSystem.getMidiDeviceInfo();
    for (int i = 0; i < allInfos.length; i++) {
      if (allInfos[i].equals(targetInfo)) {
        return i;
      }
    }
    return -1; // Not found
  }

  public static MidiDevice.Info getInfo(int index) throws MidiUnavailableException {
    MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
    if (index < 0 || index >= infos.length) {
      throw new MidiUnavailableException("Invalid device index: " + index);
    }
    return infos[index];
  }
}
