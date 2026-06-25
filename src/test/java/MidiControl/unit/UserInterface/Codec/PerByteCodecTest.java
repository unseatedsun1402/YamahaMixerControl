package MidiControl.unit.UserInterface.Codec;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import MidiControl.UserInterface.ChannelName.Codecs.PerByteCodec;

public class PerByteCodecTest {

    @Test
    public void decodesAsciiValuesInOrder() {
        PerByteCodec codec = new PerByteCodec();

        String result = codec.decode(List.of(
            (int) 'K',
            (int) 'i',
            (int) 'c',
            (int) 'k'
        ));

        assertEquals("Kick", result);
    }

    @Test
    public void trimsTrailingSpaces() {
        PerByteCodec codec = new PerByteCodec();

        String result = codec.decode(List.of(
            (int) 'S',
            (int) 'n',
            (int) 'r',
            (int) ' '
        ));

        assertEquals("Snr", result);
    }

    @Test
    public void blanksNonPrintableValues() {
        PerByteCodec codec = new PerByteCodec();

        String result = codec.decode(List.of(
            0,
            (int) 'i'
        ));

        assertEquals(" i", result);
    }

    @Test
    public void masksToSevenBitBeforeAsciiDecode() {
        PerByteCodec codec = new PerByteCodec();

        /*
         * 0xC9 & 0x7F = 0x49 = 'I'
         *
         * This proves the codec is handling MIDI-style 7-bit-safe values and
         * not simply rejecting every value above 127.
         */
        String result = codec.decode(List.of(0xC9));

        assertEquals("I", result);
    }

    @Test
    public void encodesOneBytePerCharacter() {
        PerByteCodec codec = new PerByteCodec();

        Optional<List<byte[]>> encoded = codec.encode("Kick");

        assertTrue(encoded.isPresent());
        assertEquals(4, encoded.get().size());

        assertArrayEquals(new byte[] { (byte) 'K' }, encoded.get().get(0));
        assertArrayEquals(new byte[] { (byte) 'i' }, encoded.get().get(1));
        assertArrayEquals(new byte[] { (byte) 'c' }, encoded.get().get(2));
        assertArrayEquals(new byte[] { (byte) 'k' }, encoded.get().get(3));
    }
}