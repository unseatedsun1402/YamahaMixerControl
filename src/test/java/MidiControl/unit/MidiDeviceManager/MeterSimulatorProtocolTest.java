package MidiControl.unit.MidiDeviceManager;

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
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import MidiControl.ControlServer.HardwareInputHandler;
import MidiControl.MidiDeviceManager.MidiDeviceUtils;
import MidiControl.MidiDeviceManager.Simulator.M7CLSimulatorMidiDevice;
import MidiControl.MidiDeviceManager.Simulator.O1V96iSimulatorMidiDevice;
import MidiControl.NrpnUtils.NrpnParser;
import MidiControl.NrpnUtils.NrpnRegistry;
import MidiControl.UserInterface.Meter.MeterDTO;
import MidiControl.UserInterface.Meter.MeterRequest;
import MidiControl.UserInterface.Meter.MeterSimpleParser;
import MidiControl.UserInterface.Meter.MeterUpdateListener;

class MeterSimulatorProtocolTest {
    @ParameterizedTest
    @CsvSource({
        "0, 0, 0, 1, 0, 1",
        "0, 2, 0, 24, 0, 24",
        "0, 4, 127, 0, 0, 56",
        "0, 7, 127, 0, 0, 56",
        "0, 127, 3, 1, 3, 9",
        "0, 127, 48, 1, 48, 9",
        "0, 127, 127, 0, 0, 504",
        "16, 0, 0, 1, 0, 4",
        "16, 127, 0, 1, 0, 20"
    })
    void answersM7clDatasheetExamples(int category, int source, int start, int count,
            int responseStart, int values) throws Exception {
        try (Endpoints endpoints = new Endpoints(M7CLSimulatorMidiDevice.getSimulatorInfo())) {
            byte[] response = endpoints.request(new MeterRequest(3, 0x11, category, source)
                    .setStartChannel(start).setChannelCount(count));
            assertNotNull(response);
            assertEquals(10 + values, response.length);
            assertEquals(0x13, response[2] & 0xFF);
            assertEquals(0x11, response[4] & 0xFF);
            assertEquals(category, response[6] & 0xFF);
            assertEquals(source, response[7] & 0xFF);
            assertEquals(responseStart, response[8] & 0xFF);
            if (category == 0) assertEquals(values, MeterSimpleParser.parse(response).size());
            assertNotNull(endpoints.received.poll(250, TimeUnit.MILLISECONDS),
                    "A meter request should start a stream");
        }
    }

    @ParameterizedTest
    @CsvSource({"0,0,4,3", "0,1,31,1", "1,1,2,3", "2,3,7,1", "4,0,0,2", "5,0,8,2", "6,19,3,1"})
    void answers01v96iRequestWith14BitValues(int category, int source, int start, int count) throws Exception {
        try (Endpoints endpoints = new Endpoints(O1V96iSimulatorMidiDevice.getSimulatorInfo())) {
            byte[] response = endpoints.request(new MeterRequest(2, 0x1A, category, source)
                    .setStartChannel(start).setChannelCount(count));
            assertNotNull(response);
            assertEquals(10 + count * 2, response.length);
            assertEquals(0x12, response[2] & 0xFF);
            assertEquals(0x1A, response[4] & 0xFF);
            assertEquals(category, response[6] & 0xFF);
            assertEquals(source, response[7] & 0xFF);
            assertEquals(start, response[8] & 0xFF);
            assertEquals(count, MeterSimpleParser.parse(response).size());
            assertNotNull(endpoints.received.poll(250, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    void answers01v96iLivenessProbe() throws Exception {
        try (Endpoints endpoints = new Endpoints(O1V96iSimulatorMidiDevice.getSimulatorInfo())) {
            byte[] request = {
                (byte) 0xF0, 0x43, 0x30, 0x3E, 0x7F, 0x01, 0x1C, 0, 0, (byte) 0xF7
            };
            byte[] response = endpoints.send(request);
            assertNotNull(response);
            assertEquals(14, response.length);
            assertEquals(0x10, response[2] & 0xFF);
            assertEquals(0x1C, response[6] & 0xFF);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void simulatedResponseReachesMeterListener(boolean m7cl) throws Exception {
        var updates = new LinkedBlockingQueue<MeterDTO>();
        MeterUpdateListener listener = updates::add;
        MeterSimpleParser.reset();
        MeterSimpleParser.addListener(listener);
        MidiDevice.Info info = m7cl ? M7CLSimulatorMidiDevice.getSimulatorInfo()
                : O1V96iSimulatorMidiDevice.getSimulatorInfo();
        try (Endpoints endpoints = new Endpoints(info)) {
            byte[] response = endpoints.request(new MeterRequest(0, m7cl ? 0x11 : 0x1A, 0, 0)
                    .setStartChannel(4).setChannelCount(1));
            assertNotNull(response);
            SysexMessage message = new SysexMessage();
            message.setMessage(response, response.length);
            new HardwareInputHandler(new NrpnParser(), new NrpnRegistry()).handle(message);
            MeterDTO update = updates.poll(2, TimeUnit.SECONDS);
            assertNotNull(update);
            assertEquals(4, update.offset);
            assertEquals(m7cl ? 0x11 : 0x1A, update.model & 0xFF);
        } finally {
            MeterSimpleParser.removeListener(listener);
            MeterSimpleParser.reset();
        }
    }

    @Test
    void capturedM7clResponseReachesMeterListener() throws Exception {
        byte[] response = {
            (byte) 0xF0, 0x43, 0x10, 0x3E, 0x11, 0x21, 0, 0, 0,
            0, 0x19, 2, 2, 2, 0x19, 2, 4, 0, 3, 2, 2, 3, 2, 2, 2,
            3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 2, 2, 3, 3, 2, 2, 3,
            3, 3, 2, 3, 3, 3, 4, 3, 2, 2, 5, 5, 3, 3, (byte) 0xF7
        };
        var updates = new LinkedBlockingQueue<MeterDTO>();
        MeterUpdateListener listener = updates::add;
        MeterSimpleParser.reset();
        MeterSimpleParser.addListener(listener);
        try {
            SysexMessage message = new SysexMessage();
            message.setMessage(response, response.length);
            assertNull(new HardwareInputHandler(new NrpnParser(), new NrpnRegistry()).handle(message));
            for (int channel = 0; channel < 48; channel++) {
                MeterDTO update = updates.poll(2, TimeUnit.SECONDS);
                assertNotNull(update, "Missing meter for channel " + channel);
                assertEquals(channel, update.offset);
                assertEquals(0x11, update.model & 0xFF);
                assertEquals("IN_PRE_HPF", update.source);
                if (channel == 1) assertEquals(0x19, update.value);
            }
        } finally {
            MeterSimpleParser.removeListener(listener);
            MeterSimpleParser.reset();
        }
    }

    @Test
    @Timeout(20)
    void streamsAt30HzAndRefreshExtendsTheTenSecondLifetime() throws Exception {
        try (Endpoints endpoints = new Endpoints(M7CLSimulatorMidiDevice.getSimulatorInfo())) {
            MeterRequest request = new MeterRequest(0, 0x11, 0, 0).setChannelCount(1);
            assertNotNull(endpoints.request(request));
            long firstRequest = System.nanoTime();
            int messages = 0;
            long sampleEnd = firstRequest + TimeUnit.SECONDS.toNanos(1);
            while (System.nanoTime() < sampleEnd) {
                if (endpoints.received.poll(100, TimeUnit.MILLISECONDS) != null) messages++;
            }
            assertTrue(messages >= 24 && messages <= 36, "Expected approximately 30 updates per second: " + messages);
            Thread.sleep(1000);
            endpoints.received.clear();
            assertNotNull(endpoints.request(request));
            long refreshed = System.nanoTime();
            long pastOriginalExpiry = firstRequest + TimeUnit.MILLISECONDS.toNanos(10_300);
            TimeUnit.NANOSECONDS.sleep(Math.max(0, pastOriginalExpiry - System.nanoTime()));
            endpoints.received.clear();
            assertNotNull(endpoints.received.poll(250, TimeUnit.MILLISECONDS),
                    "Refresh should extend the stream beyond the original expiry");
            long pastRefreshExpiry = refreshed + TimeUnit.MILLISECONDS.toNanos(10_300);
            TimeUnit.NANOSECONDS.sleep(Math.max(0, pastRefreshExpiry - System.nanoTime()));
            endpoints.received.clear();
            org.junit.jupiter.api.Assertions.assertNull(endpoints.received.poll(250, TimeUnit.MILLISECONDS),
                    "Stream should stop after ten seconds without a refresh");
        }
    }

    private static final class Endpoints implements AutoCloseable {
        private final MidiDevice input;
        private final MidiDevice output;
        private final Transmitter transmitter;
        private final Receiver sender;
        private final LinkedBlockingQueue<byte[]> received = new LinkedBlockingQueue<>();

        private Endpoints(MidiDevice.Info info) throws Exception {
            input = MidiDeviceUtils.getDevice(info);
            output = MidiDeviceUtils.getDevice(info);
            input.open();
            output.open();
            transmitter = input.getTransmitter();
            transmitter.setReceiver(new Receiver() {
                @Override public void send(MidiMessage message, long timestamp) {
                    received.offer(message.getMessage().clone());
                }
                @Override public void close() {}
            });
            sender = output.getReceiver();
        }

        private byte[] request(MeterRequest request) throws Exception {
            return send(request.toByteArray());
        }

        private byte[] send(byte[] bytes) throws Exception {
            SysexMessage message = new SysexMessage();
            message.setMessage(bytes, bytes.length);
            sender.send(message, -1);
            return received.poll(2, TimeUnit.SECONDS);
        }

        @Override public void close() {
            transmitter.close();
            sender.close();
            input.close();
            output.close();
            MeterSimpleParser.reset();
        }
    }
}
