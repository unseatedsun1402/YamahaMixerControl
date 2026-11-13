package MidiControl.unit.MidiDeviceManager;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Transmitter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import MidiControl.ControlServer.HardwareInputHandler;
import MidiControl.MidiDeviceManager.MidiDeviceDTO;
import MidiControl.MidiDeviceManager.MidiDeviceUtils;
import MidiControl.MidiDeviceManager.MidiIOManager;
import MidiControl.MidiDeviceManager.Simulator.O1V96iSimulatorMidiDevice;
import MidiControl.NrpnUtils.NrpnParser;
import MidiControl.NrpnUtils.NrpnRegistry;
import MidiControl.UserInterface.Meter.MeterDTO;
import MidiControl.UserInterface.Meter.MeterSimpleParser;
import MidiControl.UserInterface.Meter.MeterUpdateListener;

class O1V96iSimulatorMidiDeviceTest {
    @Test
    void simulatorIsListedAndAnswersFaderProbe() throws Exception {
        MidiDeviceDTO listed = new MidiIOManager().listDeviceDTOs().stream()
                .filter(device -> O1V96iSimulatorMidiDevice.DEVICE_NAME.equals(device.name))
                .findFirst().orElseThrow();
        assertTrue(listed.canInput);
        assertTrue(listed.canOutput);
        assertTrue(MidiDeviceUtils.getDeviceIndex(O1V96iSimulatorMidiDevice.getSimulatorInfo()) >= 0);

        try (Simulator simulator = new Simulator()) {
            simulator.send((byte) 0xF0, 0x43, 0x35, 0x3E, 0x7F, 0x01, 0x1C, 0x00, 0x08, (byte) 0xF7);
            assertArrayEquals(new byte[] {
                    (byte) 0xF0, 0x43, 0x15, 0x3E, 0x7F, 0x01, 0x1C, 0x00, 0x08,
                    0, 0, 0, 0, (byte) 0xF7
            }, simulator.receive());
        }
    }

    @ParameterizedTest
    @CsvSource({"0,0,4,3", "0,2,30,2", "1,3,2,3", "2,1,1,4",
            "4,0,0,2", "5,0,7,3", "6,12,1,3"})
    void emitsFramedFourteenBitValuesForSupportedMeterRequests(
            int category, int source, int start, int count) throws Exception {
        try (Simulator simulator = new Simulator()) {
            simulator.send((byte) 0xF0, 0x43, 0x32, 0x3E, 0x1A, 0x21,
                    (byte) category, (byte) source, (byte) start, 0, (byte) count, (byte) 0xF7);
            byte[] response = simulator.receive();
            assertNotNull(response);
            assertEquals(10 + count * 2, response.length);
            assertArrayEquals(new byte[] {(byte) 0xF0, 0x43, 0x12, 0x3E, 0x1A, 0x21,
                    (byte) category, (byte) source, (byte) start},
                    java.util.Arrays.copyOf(response, 9));
            assertEquals(0xF7, response[response.length - 1] & 0xFF);
            for (int i = 9; i < response.length - 1; i++) {
                assertEquals(0, response[i] & 0x80);
            }
            assertNotNull(simulator.receive(), "Meter stream should continue after the first frame");
        }
    }

    @Test
    void allChannelsAndUnsupportedRequests() throws Exception {
        try (Simulator simulator = new Simulator()) {
            simulator.send((byte) 0xF0, 0x43, 0x30, 0x3E, 0x1A, 0x21,
                    0, 0, 0x7F, 0, 0, (byte) 0xF7);
            byte[] response = simulator.receive();
            assertNotNull(response);
            assertEquals(90, response.length);
            assertEquals(0, response[8]);
        }
        try (Simulator simulator = new Simulator()) {
            simulator.send((byte) 0xF0, 0x43, 0x30, 0x3E, 0x1A, 0x21,
                    0, 0x7F, 0, 0, 1, (byte) 0xF7);
            simulator.send((byte) 0xF0, 0x43, 0x30, 0x3E, 0x1A, 0x21,
                    0, 0, 39, 0, 2, (byte) 0xF7);
            assertNull(simulator.received.poll(150, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    void simulatedMetersReachHardwareParserAndListener() throws Exception {
        LinkedBlockingQueue<MeterDTO> updates = new LinkedBlockingQueue<>();
        MeterUpdateListener listener = updates::add;
        MeterSimpleParser.setRequestedRange(4, 1);
        MeterSimpleParser.addListener(listener);
        try (Simulator simulator = new Simulator()) {
            simulator.send((byte) 0xF0, 0x43, 0x30, 0x3E, 0x1A, 0x21,
                    0, 0, 4, 0, 1, (byte) 0xF7);
            byte[] response = simulator.receive();
            assertNotNull(response);
            HardwareInputHandler handler = new HardwareInputHandler(new NrpnParser(), new NrpnRegistry());
            assertNull(handler.handle(new SysexMessage(response, response.length)));
            MeterDTO update;
            do {
                update = updates.poll(2, TimeUnit.SECONDS);
                assertNotNull(update);
            } while ((update.model & 0xFF) != 0x1A || update.offset != 4);
            assertEquals(((response[9] & 0x7F) << 7) | (response[10] & 0x7F), update.value);
        } finally {
            MeterSimpleParser.removeListener(listener);
            MeterSimpleParser.setRequestedRange(0, 128);
        }
    }

    private static final class Simulator implements AutoCloseable {
        private final MidiDevice input;
        private final MidiDevice output;
        private final Transmitter transmitter;
        private final Receiver outputReceiver;
        private final LinkedBlockingQueue<byte[]> received = new LinkedBlockingQueue<>();

        private Simulator() throws Exception {
            input = MidiDeviceUtils.getDevice(O1V96iSimulatorMidiDevice.getSimulatorInfo());
            output = MidiDeviceUtils.getDevice(O1V96iSimulatorMidiDevice.getSimulatorInfo());
            input.open();
            output.open();
            transmitter = input.getTransmitter();
            transmitter.setReceiver(new Receiver() {
                @Override public void send(MidiMessage message, long timeStamp) {
                    received.offer(message.getMessage().clone());
                }
                @Override public void close() {}
            });
            outputReceiver = output.getReceiver();
        }

        private void send(int... values) throws Exception {
            byte[] bytes = new byte[values.length];
            for (int i = 0; i < values.length; i++) {
                bytes[i] = (byte) values[i];
            }
            outputReceiver.send(new SysexMessage(bytes, bytes.length), -1);
        }

        private byte[] receive() throws InterruptedException {
            return received.poll(2, TimeUnit.SECONDS);
        }

        @Override public void close() {
            transmitter.close();
            outputReceiver.close();
            input.close();
            output.close();
        }
    }
}
