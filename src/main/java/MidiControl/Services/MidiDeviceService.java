package MidiControl.Services;

import MidiControl.MidiDeviceManager.MidiDeviceUtils;

import javax.sound.midi.MidiDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class MidiDeviceService {

    private static final Logger logger = Logger.getLogger(MidiDeviceService.class.getName());

    private final MidiDeviceProvider provider;

    // Production constructor
    public MidiDeviceService() {
        this.provider = new SystemMidiDeviceProvider();
    }

    // Test constructor
    public MidiDeviceService(MidiDeviceProvider provider) {
        this.provider = provider;
    }

    public static class DeviceInfoDTO {
        public int index;
        public String name;
        public String description;
        public boolean isInput;
        public boolean isOutput;

        public DeviceInfoDTO(int index, String name, String description,
                             boolean isInput, boolean isOutput) {
            this.index = index;
            this.name = name;
            this.description = description;
            this.isInput = isInput;
            this.isOutput = isOutput;
        }
    }

    public List<DeviceInfoDTO> getInputDevices() {
        List<DeviceInfoDTO> result = new ArrayList<>();

        MidiDevice.Info[] infos = provider.getDeviceInfos();

        for (MidiDevice.Info info : infos) {
            try {
                MidiDevice device = provider.getDevice(info);

                boolean isInput = device.getMaxTransmitters() != 0;
                if (!isInput) continue;

                int index = MidiDeviceUtils.getDeviceIndex(info);

                result.add(new DeviceInfoDTO(
                        index,
                        info.getName(),
                        info.getDescription(),
                        true,
                        false
                ));

            } catch (Exception e) {
                logger.warning("Failed to inspect device: " + info.getName());
            }
        }

        return result;
    }

    public List<DeviceInfoDTO> getOutputDevices() {
        List<DeviceInfoDTO> result = new ArrayList<>();

        MidiDevice.Info[] infos = provider.getDeviceInfos();

        for (MidiDevice.Info info : infos) {
            try {
                MidiDevice device = provider.getDevice(info);

                boolean isOutput = device.getMaxReceivers() != 0;
                if (!isOutput) continue;

                int index = MidiDeviceUtils.getDeviceIndex(info);

                result.add(new DeviceInfoDTO(
                        index,
                        info.getName(),
                        info.getDescription(),
                        false,
                        true
                ));

            } catch (Exception e) {
                logger.warning("Failed to inspect device: " + info.getName());
            }
        }

        return result;
    }
}