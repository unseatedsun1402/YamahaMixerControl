package MidiControl.unit.UserInterface.Meter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import MidiControl.UserInterface.Meter.MeterDTO;
import MidiControl.UserInterface.Meter.MeterSimpleParser;
import MidiControl.UserInterface.Meter.MeterUpdateListener;

class MeterSimpleParserTest {

    @Test
    void publishesLatestValueForMeterAfterQueueDrain() throws Exception {
        LinkedBlockingQueue<MeterDTO> updates = new LinkedBlockingQueue<>();
        MeterUpdateListener listener = updates::add;
        MeterSimpleParser.addListener(listener);
        MeterSimpleParser.setRequestedRange(126, 1);

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
        MeterSimpleParser.setRequestedRange(4, 1);
        byte[] response = {
                (byte) 0xF0, 0x43, 0x10, 0x3E, 0x1A, 0x21,
                0x00, 0x00, 0x04, 0x00, 0x14, (byte) 0xF7
        };

        assertEquals(1, MeterSimpleParser.parse(response).size());
        assertEquals(20, MeterSimpleParser.parse(response).get(0));
    }

    private byte[] meterMessage(int value) {
        return new byte[] {
                (byte) 0xF0, 0x43, 0x10, 0x3E, 0x0F, 0x21,
                0x00, 0x00, 0x7E, (byte) value, (byte) 0xF7
        };
    }
}
