package MidiControl.unit.UserInterface.Meter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.sound.midi.SysexMessage;

import org.junit.jupiter.api.Test;

import MidiControl.MidiDeviceManager.MidiInputReceiver;
import MidiControl.UserInterface.Meter.MeterDiagnostics;
import MidiControl.UserInterface.Meter.MeterRequest;

class MeterDiagnosticsTest {
    @Test
    void logsRequestsAndSamplesUnrecognizedMeterLikeResponses() throws Exception {
        Logger logger = Logger.getLogger(MeterDiagnostics.class.getName());
        List<String> messages = new ArrayList<>();
        Handler handler = new Handler() {
            @Override public void publish(LogRecord record) { messages.add(record.getMessage()); }
            @Override public void flush() {}
            @Override public void close() {}
        };
        var field = MeterDiagnostics.class.getDeclaredField("lastCandidate");
        field.setAccessible(true);
        AtomicLong lastCandidate = (AtomicLong) field.get(null);
        long previous = lastCandidate.getAndSet(0);
        logger.addHandler(handler);
        try {
            byte[] request = new MeterRequest(3, 0x11, 0, 0).setChannelCount(48).toByteArray();
            MeterDiagnostics.requestQueued("YAMAHA_M7CL", request);
            MeterDiagnostics.requestSent("Test MIDI", request);

            byte[] bytes = {(byte) 0xF0, 0x43, 0x13, 0x3E, 0x11, 0x21, 0, 0, 0, 1, (byte) 0xF7};
            SysexMessage response = new SysexMessage();
            response.setMessage(bytes, bytes.length);
            MidiInputReceiver input = new MidiInputReceiver(new ConcurrentLinkedQueue<>());
            input.send(response, -1);
            input.send(response, -1);

            assertTrue(messages.stream().anyMatch(m -> m.contains("queued profile=YAMAHA_M7CL")
                    && m.contains("F0 43 33 3E 11 21")));
            assertTrue(messages.stream().anyMatch(m -> m.contains("MIDI output=Test MIDI")));
            assertEquals(1, messages.stream().filter(m -> m.contains("MIDI input meter-like")
                    && m.contains("F0 43 13 3E 11 21")).count());
        } finally {
            logger.removeHandler(handler);
            lastCandidate.set(previous);
        }
    }
}
