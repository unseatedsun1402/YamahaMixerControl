package MidiControl.unit.MidiDeviceManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Transmitter;

import org.junit.jupiter.api.Test;

import MidiControl.MidiDeviceManager.MidiDeviceDTO;
import MidiControl.MidiDeviceManager.MidiIOManager;
import MidiControl.MidiDeviceManager.MidiDeviceUtils;
import MidiControl.MidiDeviceManager.Simulator.M7CLSimulatorMidiDevice;

class M7CLSimulatorMidiDeviceTest {

    @Test
    void simulatorAppearsInMidiCatalogAndAnswersMeterSysEx() throws Exception {
        MidiDevice.Info[] infos = MidiDeviceUtils.getDeviceInfos();
        int simulatorIndex = -1;
        for (int i = 0; i < infos.length; i++) {
            if (M7CLSimulatorMidiDevice.DEVICE_NAME.equals(infos[i].getName())) {
                simulatorIndex = i;
                break;
            }
        }

        assertTrue(simulatorIndex >= 0, "Simulator must be discoverable as a MIDI device");
        MidiDeviceDTO listedSimulator = new MidiIOManager().listDeviceDTOs().stream()
                .filter(device -> M7CLSimulatorMidiDevice.DEVICE_NAME.equals(device.name))
                .findFirst()
                .orElseThrow();
        assertTrue(listedSimulator.canInput);
        assertTrue(listedSimulator.canOutput);

        MidiDevice input = MidiDeviceUtils.getDevice(simulatorIndex);
        MidiDevice output = MidiDeviceUtils.getDevice(simulatorIndex);
        LinkedBlockingQueue<byte[]> received = new LinkedBlockingQueue<>();
        input.open();
        output.open();

        Transmitter transmitter = input.getTransmitter();
        Receiver inputReceiver = new Receiver() {
            @Override
            public void send(MidiMessage message, long timeStamp) {
                received.offer(message.getMessage().clone());
            }

            @Override
            public void close() {}
        };
        transmitter.setReceiver(inputReceiver);

        Receiver outputReceiver = output.getReceiver();
        try {
            SysexMessage request = new SysexMessage();
            byte[] requestBytes = {
                    (byte) 0xF0, 0x43, 0x30, 0x3E, 0x11, 0x21,
                    0x00, 0x00, 0x00, 0x00, 0x20, (byte) 0xF7
            };
            request.setMessage(requestBytes, requestBytes.length);
            outputReceiver.send(request, -1);

            byte[] response = received.poll(2, TimeUnit.SECONDS);
            assertNotNull(response, "Meter request should produce simulated meter SysEx");
            assertEquals(42, response.length);
            assertEquals(0x10, response[2] & 0xFF);
            assertEquals(0x0F, response[4] & 0xFF);
            assertEquals(0x21, response[5] & 0xFF);
            assertEquals(0x00, response[6] & 0xFF);
            assertEquals(0x00, response[7] & 0xFF);
            assertEquals(0x00, response[8] & 0xFF);
            assertEquals(0xF7, response[response.length - 1] & 0xFF);
            for (int i = 9; i < response.length - 1; i++) {
                assertTrue((response[i] & 0x80) == 0, "SysEx meter data must be 7-bit");
            }

            SysexMessage livenessProbe = new SysexMessage();
            byte[] livenessProbeBytes = {
                    (byte) 0xF0, 0x43, 0x30, 0x3E, 0x11, 0x01,
                    0x00, 0x32, 0x00, 0x00, 0x00, 0x00, (byte) 0xF7
            };
            livenessProbe.setMessage(livenessProbeBytes, livenessProbeBytes.length);
            outputReceiver.send(livenessProbe, -1);

            byte[] livenessResponse;
            do {
                livenessResponse = received.poll(2, TimeUnit.SECONDS);
            } while (livenessResponse != null && (livenessResponse[5] & 0xFF) == 0x21);
            assertNotNull(livenessResponse, "M7CL liveness probe should receive a parameter response");
            assertEquals(18, livenessResponse.length);
            assertEquals(0x10, livenessResponse[2] & 0xFF);
            assertEquals(0x3E, livenessResponse[3] & 0xFF);
            assertEquals(0x11, livenessResponse[4] & 0xFF);
            assertEquals(0x32, livenessResponse[7] & 0xFF);
            assertEquals(0xF7, livenessResponse[livenessResponse.length - 1] & 0xFF);
            for (int i = 12; i < livenessResponse.length - 1; i++) {
                assertEquals(0, livenessResponse[i] & 0x7F,
                        "Simulated fader response should encode the minimum fader value");
            }
        } finally {
            transmitter.close();
            inputReceiver.close();
            outputReceiver.close();
            input.close();
            output.close();
        }
    }
}
