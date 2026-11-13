package MidiControl.unit.UserInterface.Meter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import javax.sound.midi.SysexMessage;
import MidiControl.ControlServer.HardwareInputHandler;
import MidiControl.NrpnUtils.NrpnParser;
import MidiControl.NrpnUtils.NrpnRegistry;
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

    @Test
    void capturedM7clResponseReachesMeterListener() throws Exception {
        byte[] response = {
                (byte) 0xF0, 0x43, 0x10, 0x3E, 0x11, 0x21, 0, 0, 0,
                0, 0x19, 2, 2, 2, 0x19, 2, 4, 0, 3, 2, 2, 3, 2, 2, 2,
                3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 2, 2, 3, 3, 2, 2, 3,
                3, 3, 2, 3, 3, 3, 4, 3, 2, 2, 5, 5, 3, 3, (byte) 0xF7
        };
        LinkedBlockingQueue<MeterDTO> updates = new LinkedBlockingQueue<>();
        MeterUpdateListener listener = updates::add;
        MeterSimpleParser.setRequestedRange(0, 48);
        MeterSimpleParser.addListener(listener);
        try {
            SysexMessage message = new SysexMessage(response, response.length);
            assertNull(new HardwareInputHandler(new NrpnParser(), new NrpnRegistry()).handle(message));
            for (int channel = 0; channel < 48; channel++) {
                MeterDTO update;
                do {
                    update = updates.poll(2, TimeUnit.SECONDS);
                    assertNotNull(update, "Missing meter for channel " + channel);
                } while ((update.model & 0xFF) != 0x11);
                assertEquals(channel, update.offset);
                assertEquals(response[9 + channel] & 0x7F, update.value);
                assertEquals("CHANNEL", update.source);
            }
        } finally {
            MeterSimpleParser.removeListener(listener);
            MeterSimpleParser.setRequestedRange(0, 128);
        }
    }

    private byte[] meterMessage(int value) {
        return new byte[] {
                (byte) 0xF0, 0x43, 0x10, 0x3E, 0x0F, 0x21,
                0x00, 0x00, 0x7E, (byte) value, (byte) 0xF7
        };
    }
}
