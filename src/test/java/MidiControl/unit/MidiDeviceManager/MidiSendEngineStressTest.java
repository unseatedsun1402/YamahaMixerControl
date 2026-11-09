package MidiControl.unit.MidiDeviceManager;

import MidiControl.MidiDeviceManager.MidiOutput;
import MidiControl.MidiDeviceManager.MidiSendEngine;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("stress")
class MidiSendEngineStressTest {

    private static final int QUEUE_CAPACITY = 2048;
    private static final long SCENARIO_NANOS = TimeUnit.MILLISECONDS.toNanos(500);
    private static final long DRAIN_NANOS = TimeUnit.MILLISECONDS.toNanos(100);
    private static final int[] MESSAGE_LENGTHS = {3, 14};
    private static final long[] DEVICE_DELAYS_MICROS = {0, 1000};
    private static final int[] PRODUCER_RATES = {125, 250, 500};

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void reportsQueuePressureAcrossProfilesPayloadsAndDeviceLatency() {
        System.out.println("profile,payloadBytes,deviceDelayUs,targetPerSecond,attempted,accepted,"
                + "rejected,sent,pending,peakQueue,actualPerSecond");
        MidiSendEngine.ThroughputProfile[] profiles = {
                MidiSendEngine.ThroughputProfile.SAFE_DIN,
                MidiSendEngine.ThroughputProfile.FAST_USB
        };
        for (MidiSendEngine.ThroughputProfile profile : profiles) {
            for (int messageLength : MESSAGE_LENGTHS) {
                for (long deviceDelayMicros : DEVICE_DELAYS_MICROS) {
                    for (int producerRate : PRODUCER_RATES) {
                        runScenario(profile, messageLength, deviceDelayMicros, producerRate);
                    }
                }
            }
        }
    }

    private static void runScenario(
            MidiSendEngine.ThroughputProfile profile,
            int messageLength,
            long deviceDelayMicros,
            int producerRate) {
        CountingMidiOutput output = new CountingMidiOutput(deviceDelayMicros);
        MidiSendEngine engine = new MidiSendEngine(output, QUEUE_CAPACITY, QUEUE_CAPACITY);
        engine.setThroughputProfile(profile);
        engine.start();

        long start = System.nanoTime();
        long periodNanos = TimeUnit.SECONDS.toNanos(1) / producerRate;
        long end = start + SCENARIO_NANOS;
        long nextOffer = start;
        int attempted = 0;
        long accepted = 0;
        int peakQueue = 0;

        try {
            while (System.nanoTime() < end) {
                if (engine.offer(message(messageLength, attempted))) {
                    accepted++;
                }
                attempted++;
                peakQueue = Math.max(peakQueue, engine.getQueuedMessageCount());
                nextOffer += periodNanos;
                long remaining = nextOffer - System.nanoTime();
                if (remaining > 0) {
                    LockSupport.parkNanos(remaining);
                } else if (remaining < -periodNanos * 4) {
                    nextOffer = System.nanoTime();
                }
            }

            long offeredNanos = System.nanoTime() - start;
            long drainUntil = System.nanoTime() + DRAIN_NANOS;
            while (System.nanoTime() < drainUntil) {
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
            }
            long sent = output.sentCount();
            long rejected = attempted - accepted;
            long pending = Math.max(0, accepted - sent);
            System.out.printf(
                    "%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%.0f%n",
                    profile,
                    messageLength,
                    deviceDelayMicros,
                    producerRate,
                    attempted,
                    accepted,
                    rejected,
                    sent,
                    pending,
                    peakQueue,
                    attempted * 1_000_000_000.0 / offeredNanos);
            assertTrue(attempted > 0, "Stress producer should offer messages");
        } finally {
            engine.stop();
        }
    }

    private static byte[] message(int length, int sequence) {
        byte[] message = new byte[length];
        if (length == 3) {
            message[0] = (byte) 0xB0;
            message[1] = (byte) (sequence & 0x7F);
            message[2] = (byte) ((sequence >>> 7) & 0x7F);
            return message;
        }

        message[0] = (byte) 0xF0;
        for (int i = 1; i < length - 1; i++) {
            message[i] = (byte) ((sequence + i) & 0x7F);
        }
        message[length - 1] = (byte) 0xF7;
        return message;
    }

    private static final class CountingMidiOutput implements MidiOutput {
        private final long delayNanos;
        private final LongAdder sent = new LongAdder();

        private CountingMidiOutput(long delayMicros) {
            this.delayNanos = TimeUnit.MICROSECONDS.toNanos(delayMicros);
        }

        @Override
        public void sendMessage(byte[] message) {
            sent.increment();
            if (delayNanos > 0) {
                LockSupport.parkNanos(delayNanos);
            }
        }

        @Override public void sendMessage(MidiMessage message) {
            throw new UnsupportedOperationException();
        }

        @Override public MidiDevice.Info getDeviceInfo() {
            return new MidiDevice.Info("stress", "test", "synthetic output", "1") {};
        }

        @Override public boolean isOpen() { return true; }
        @Override public void close() {}

        private long sentCount() {
            return sent.sum();
        }
    }
}
