package MidiControl.unit.UserInterface.Meter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import MidiControl.UserInterface.Meter.MeterRequest;

public class MeterRequestTest {

    @Tag("Unit")
    @Test
    public void meterRequestDefaultToByteArray() {
        MeterRequest req = new MeterRequest(0, 0x01, 0x02, 0x03);

        byte[] expected = new byte[] {
            (byte) 0xF0,
            (byte) 0x43,
            (byte) 0x30,
            (byte) 0x3E,
            (byte) 0x01,
            (byte) 0x21,
            (byte) 0x02,
            (byte) 0x03,
            (byte) 0x00,
            (byte) 0x00,
            (byte) 0x01,
            (byte) 0xF7
        };

        assertArrayEquals(expected, req.toByteArray());
        assertEquals(12, req.toByteArray().length);
    }

    @Tag("Unit")
    @Test
    public void meterRequestChannelEncoding() {
        MeterRequest ch0 = new MeterRequest(0, 0x01, 0x02, 0x03);
        MeterRequest ch15 = new MeterRequest(15, 0x01, 0x02, 0x03);

        byte[] a0 = ch0.toByteArray();
        byte[] a15 = ch15.toByteArray();

        assertEquals((byte) 0x30, a0[2]);
        assertEquals((byte) 0x3F, a15[2]);
        assertNotEquals(a0[2], a15[2]);
    }

    @Tag("Unit")
    @Test
    public void meterRequestSetStartChannel() {
        MeterRequest req = new MeterRequest(1, 0x01, 0x02, 0x03).setStartChannel(127);
        assertEquals((byte) 0x7F, req.toByteArray()[8]);
    }

    @Tag("Unit")
    @Test
    public void meterRequestChannelCountEncoding14Bit() {
        MeterRequest req = new MeterRequest(2, 0x01, 0x02, 0x03).setChannelCount(0x1234);

        byte[] arr = req.toByteArray();

        assertEquals((byte) 0x24, arr[9]);
        assertEquals((byte) 0x34, arr[10]);
    }

    @Tag("Unit")
    @Test
    public void meterRequestToHexString() {
        MeterRequest req = new MeterRequest(0, 0x01, 0x02, 0x03);
        assertEquals("F0 43 30 3E 01 21 02 03 00 00 01 F7", req.toHexString());
    }

    @Tag("Unit")
    @Test
    public void meterRequestConstructorRejectsInvalidChannel() {
        assertThrows(IllegalArgumentException.class, () -> new MeterRequest(-1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new MeterRequest(16, 0, 0, 0));
    }

    @Tag("Unit")
    @Test
    public void meterRequestConstructorRejectsInvalid7BitFields() {
        assertThrows(IllegalArgumentException.class, () -> new MeterRequest(0, -1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new MeterRequest(0, 128, 0, 0));

        assertThrows(IllegalArgumentException.class, () -> new MeterRequest(0, 0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new MeterRequest(0, 0, 128, 0));

        assertThrows(IllegalArgumentException.class, () -> new MeterRequest(0, 0, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> new MeterRequest(0, 0, 0, 128));
    }

    @Tag("Unit")
    @Test
    public void meterRequestSetStartChannelRejectsOutOfRange() {
        MeterRequest req = new MeterRequest(0, 0x01, 0x02, 0x03);

        assertThrows(IllegalArgumentException.class, () -> req.setStartChannel(-1));
        assertThrows(IllegalArgumentException.class, () -> req.setStartChannel(128));
    }

    @Tag("Unit")
    @Test
    public void meterRequestSetChannelCountRejectsAbove14Bit() {
        MeterRequest req = new MeterRequest(0, 0x01, 0x02, 0x03);
        assertThrows(IllegalArgumentException.class, () -> req.setChannelCount(0x4000));
    }

    @Tag("Unit")
    @Test
    public void meterRequestOffsetsDoNotAffectOutputWithCurrentImplementation() {
        MeterRequest base = new MeterRequest(0, 0x01, 0x02, 0x03);
        MeterRequest withOffsets = new MeterRequest(0, 0x01, 0x02, 0x03)
                .addOffsets(10, 20, 30)
                .addVector(40, 50);

        assertArrayEquals(base.toByteArray(), withOffsets.toByteArray());
        assertEquals(12, withOffsets.toByteArray().length);
    }
}
