package MidiControl.unit.UserInterface.Meter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import MidiControl.UserInterface.Meter.MeterDTO;
import MidiControl.UserInterface.Meter.MeterSimpleParser;
import MidiControl.UserInterface.Meter.MeterUpdateListener;

class MeterSimpleParserTest {

    @Test
    void publishesLatestValueForMeterAfterQueueDrain() throws Exception {
        LinkedBlockingQueue<MeterDTO> updates = new LinkedBlockingQueue<>();
        MeterUpdateListener listener = updates::add;
        MeterSimpleParser.addListener(listener);

        try {
            MeterSimpleParser.parse(meterMessage(10));
            MeterSimpleParser.parse(meterMessage(20));
            MeterSimpleParser.parse(meterMessage(30));

            MeterDTO latest = null;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (System.nanoTime() < deadline) {
                MeterDTO update = updates.poll(100, TimeUnit.MILLISECONDS);
                if (update != null) {
                    latest = update;
                }
                if (latest != null && latest.value == 30) {
                    break;
                }
            }

            assertTrue(latest != null, "Meter queue should publish an update");
            assertEquals(30, latest.value, "Stale readings should be replaced by the newest value");
            assertEquals(126, latest.offset);
        } finally {
            MeterSimpleParser.removeListener(listener);
        }
    }

    @Test
    void parses01v96iMetersAs14BitValuesAtRequestedStartChannel() {
        byte[] response = {
                (byte) 0xF0, 0x43, 0x10, 0x3E, 0x1A, 0x21,
                0x00, 0x00, 0x04, 0x00, 0x14, (byte) 0xF7
        };

        assertEquals(1, MeterSimpleParser.parse(response).size());
        assertEquals(20, MeterSimpleParser.parse(response).get(0));
    }

    @ParameterizedTest
    @ValueSource(ints = {0x0F, 0x11})
    void parsesM7clAllPointsWithoutTreatingPointsAsChannels(int model) throws Exception {
        LinkedBlockingQueue<MeterDTO> updates = new LinkedBlockingQueue<>();
        MeterUpdateListener listener = updates::add;
        MeterSimpleParser.reset();
        MeterSimpleParser.addListener(listener);
        try {
            byte[] response = {
                (byte) 0xF0, 0x43, 0x10, 0x3E, (byte) model, 0x21, 0, 0x7F, 3,
                1, 2, 3, 4, 5, 6, 7, 8, 9, (byte) 0xF7
            };
            assertEquals(9, MeterSimpleParser.parse(response).size());
            java.util.List<MeterDTO> result = new java.util.ArrayList<>();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (result.size() < 9 && System.nanoTime() < deadline) {
                MeterDTO dto = updates.poll(100, TimeUnit.MILLISECONDS);
                if (dto != null) result.add(dto);
            }
            assertEquals(9, result.size());
            assertTrue(result.stream().allMatch(dto -> dto.offset == 3));
            assertEquals("IN_PRE_HPF", result.get(0).source);
            assertEquals("IN_DYN2_GR", result.get(8).source);
        } finally {
            MeterSimpleParser.removeListener(listener);
            MeterSimpleParser.reset();
        }
    }

    @Test
    void rejectsTruncated14BitAndSigmaClipBitmaps() {
        assertNull(MeterSimpleParser.parse(new byte[] {
            (byte) 0xF0, 0x43, 0x10, 0x3E, 0x1A, 0x21, 0, 0, 0, 1, (byte) 0xF7
        }));
        assertNull(MeterSimpleParser.parse(new byte[] {
            (byte) 0xF0, 0x43, 0x10, 0x3E, 0x0F, 0x21, 0x10, 0, 0, 1, 2, 3, 4, (byte) 0xF7
        }));
        assertNull(MeterSimpleParser.parse(new byte[] {
            (byte) 0xF0, 0x43, 0x10, 0x3E, 0x11, 0x21, 0x10, 0, 0, 1, 2, 3, 4, (byte) 0xF7
        }));
    }

    private byte[] meterMessage(int value) {
        return new byte[] {
                (byte) 0xF0, 0x43, 0x10, 0x3E, 0x0F, 0x21,
                0x00, 0x00, 0x7E, (byte) value, (byte) 0xF7
        };
    }
}
