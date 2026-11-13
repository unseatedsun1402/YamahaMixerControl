package MidiControl.MidiDeviceManager;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import java.util.Arrays;

import MidiControl.MidiDeviceManager.Simulator.M7CLSimulatorMidiDevice;
import MidiControl.MidiDeviceManager.Simulator.O1V96iSimulatorMidiDevice;

public final class MidiDeviceUtils {

  private MidiDeviceUtils() {} // prevent instantiation
  public static MidiDevice.Info[] overrideDeviceInfos = null;
  public static MidiDevice overrideDevice = null;

  public static MidiDevice.Info[] getDeviceInfos() {
    MidiDevice.Info[] physicalInfos = MidiSystem.getMidiDeviceInfo();
    MidiDevice.Info[] simulatorInfos = {
        M7CLSimulatorMidiDevice.getSimulatorInfo(), O1V96iSimulatorMidiDevice.getSimulatorInfo()
    };
    MidiDevice.Info[] infos = Arrays.copyOf(
        physicalInfos, physicalInfos.length + simulatorInfos.length);
    System.arraycopy(simulatorInfos, 0, infos, physicalInfos.length, simulatorInfos.length);
    return infos;
  }

  public static MidiDevice getDevice(int index) throws MidiUnavailableException {
    MidiDevice.Info[] infos = getDeviceInfos();
    if (index < 0 || index >= infos.length) {
      throw new MidiUnavailableException("Invalid device index: " + index);
    }
    return getDevice(infos[index]);
  }

  public static MidiDevice getDevice(MidiDevice.Info info) throws MidiUnavailableException {
    if (M7CLSimulatorMidiDevice.isSimulatorInfo(info)) {
      return new M7CLSimulatorMidiDevice();
    }
    if (O1V96iSimulatorMidiDevice.isSimulatorInfo(info)) {
      return new O1V96iSimulatorMidiDevice();
    }
    return MidiSystem.getMidiDevice(info);
  }

  public static int getDeviceIndex(MidiDevice.Info targetInfo) {
    MidiDevice.Info[] allInfos = getDeviceInfos();
    for (int i = 0; i < allInfos.length; i++) {
      if (allInfos[i].equals(targetInfo)) {
        return i;
      }
    }
    return -1; // Not found
  }

  public static MidiDevice.Info getInfo(int index) throws MidiUnavailableException {
    MidiDevice.Info[] infos = getDeviceInfos();
    if (index < 0 || index >= infos.length) {
      throw new MidiUnavailableException("Invalid device index: " + index);
    }
    return infos[index];
  }
}
