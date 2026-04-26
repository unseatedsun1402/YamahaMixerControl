package MidiControl.functional.Services;

import MidiControl.Services.MidiDeviceService;

import org.junit.jupiter.api.Test;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MidiDeviceServiceTest {

    @Test
    public void getInputDevices_returnsConsistentInputDevices() throws Exception {
        MidiDeviceService service = new MidiDeviceService();

        List<MidiDeviceService.DeviceInfoDTO> inputs =
                assertDoesNotThrow(service::getInputDevices);

        assertNotNull(inputs);

        for (MidiDeviceService.DeviceInfoDTO dto : inputs) {
            // Basic DTO correctness
            assertTrue(dto.isInput);
            assertFalse(dto.isOutput);
            assertNotNull(dto.name);
            assertNotNull(dto.description);

            // Index must correspond to a real system device
            assertTrue(dto.index >= 0);

            MidiDevice.Info[] systemInfos = MidiSystem.getMidiDeviceInfo();
            assertTrue(dto.index < systemInfos.length);

            MidiDevice device = MidiSystem.getMidiDevice(systemInfos[dto.index]);
            assertTrue(device.getMaxTransmitters() != 0,
                    "Reported input device must support transmitters");
        }
    }

    @Test
    public void getOutputDevices_returnsConsistentOutputDevices() throws Exception {
        MidiDeviceService service = new MidiDeviceService();

        List<MidiDeviceService.DeviceInfoDTO> outputs =
                assertDoesNotThrow(service::getOutputDevices);

        assertNotNull(outputs);

        for (MidiDeviceService.DeviceInfoDTO dto : outputs) {
            // Basic DTO correctness
            assertFalse(dto.isInput);
            assertTrue(dto.isOutput);
            assertNotNull(dto.name);
            assertNotNull(dto.description);

            // Index must correspond to a real system device
            assertTrue(dto.index >= 0);

            MidiDevice.Info[] systemInfos = MidiSystem.getMidiDeviceInfo();
            assertTrue(dto.index < systemInfos.length);

            MidiDevice device = MidiSystem.getMidiDevice(systemInfos[dto.index]);
            assertTrue(device.getMaxReceivers() != 0,
                    "Reported output device must support receivers");
        }
    }

    @Test
    public void getInputDevices_neverThrows_evenIfNoInputsExist() {
        MidiDeviceService service = new MidiDeviceService();

        assertDoesNotThrow(() -> {
            List<MidiDeviceService.DeviceInfoDTO> inputs = service.getInputDevices();
            assertNotNull(inputs);
        });
    }

    @Test
    public void getOutputDevices_neverThrows_evenIfNoOutputsExist() {
        MidiDeviceService service = new MidiDeviceService();

        assertDoesNotThrow(() -> {
            List<MidiDeviceService.DeviceInfoDTO> outputs = service.getOutputDevices();
            assertNotNull(outputs);
        });
    }
}