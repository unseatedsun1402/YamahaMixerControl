package MidiControl.unit.UserInterface;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import MidiControl.SysexUtils.SysexParser;
import MidiControl.UserInterface.ChannelName.Codecs.M7clNameCodec;

public class M7clCodecTest {

    @Test
    public void decodesM7clNameChunks() {
        byte[] short1 = { 0x07, 0x23, 0x15, 0x66, 0x74 };
        byte[] short2 = { 0x03, 0x08, 0x00, 0x00, 0x00 };

        assertEquals("test", M7clNameCodec.decodeChunk(short1));
        assertEquals("1", M7clNameCodec.decodeChunk(short2).trim());
        assertEquals("test1", M7clNameCodec.decodeName(short1, short2));
    }

    @Test
    public void encodesM7clKnownChunks() {
        assertChunk("test", new byte[] { 0x07, 0x23, 0x15, 0x66, 0x74 });
        assertChunk("1",    new byte[] { 0x03, 0x08, 0x00, 0x00, 0x00 });

        assertChunk("aaaa", new byte[] { 0x06, 0x0B, 0x05, 0x42, 0x61 });
        assertChunk("bbbb", new byte[] { 0x06, 0x13, 0x09, 0x44, 0x62 });

        assertChunk("AAAA", new byte[] { 0x04, 0x0A, 0x05, 0x02, 0x41 });
        assertChunk("BBBB", new byte[] { 0x04, 0x12, 0x09, 0x04, 0x42 });

        assertChunk("1111", new byte[] { 0x03, 0x09, 0x44, 0x62, 0x31 });
        assertChunk("0000", new byte[] { 0x03, 0x01, 0x40, 0x60, 0x30 });

        assertChunk("....", new byte[] { 0x02, 0x71, 0x38, 0x5C, 0x2E });
        assertChunk("----", new byte[] { 0x02, 0x69, 0x34, 0x5A, 0x2D });
    }

    @Test
    public void decodesCapturedFullNames() {
        byte[] aaa = { 0x04, 0x0A, 0x05, 0x02, 0x20 };
        byte[] bbb = { 0x04, 0x12, 0x09, 0x04, 0x00 };

        assertEquals("AAA ", M7clNameCodec.decodeChunk(aaa));
        assertEquals("BBB ", M7clNameCodec.decodeChunk(bbb));
        assertEquals("AAA BBB", M7clNameCodec.decodeName(aaa, bbb));

        byte[] dots = { 0x02, 0x71, 0x38, 0x5C, 0x2E };

        assertEquals("....", M7clNameCodec.decodeChunk(dots));
        assertEquals("....BBB", M7clNameCodec.decodeName(dots, bbb));
    }

    @Test
    public void returnsOptionalNameOnlyWhenDecodedNameIsNotBlank() {
        byte[] blank1 = { 0x00, 0x00, 0x00, 0x00, 0x00 };
        byte[] blank2 = { 0x00, 0x00, 0x00, 0x00, 0x00 };

        Optional<String> blank = M7clNameCodec.tryDecodeName(blank1, blank2);

        assertTrue(blank.isEmpty(), "Blank M7CL names should not produce a present Optional");

        byte[] short1 = { 0x07, 0x23, 0x15, 0x66, 0x74 };
        byte[] short2 = { 0x03, 0x08, 0x00, 0x00, 0x00 };

        Optional<String> name = M7clNameCodec.tryDecodeName(short1, short2);

        assertTrue(name.isPresent());
        assertEquals("test1", name.get());
    }

    @Test
    public void tryEncodeChunkReturnsOptionalForValidInput() {
        Optional<byte[]> encoded = M7clNameCodec.tryEncodeChunk("test");

        assertTrue(encoded.isPresent());
        assertArrayEquals(
            new byte[] { 0x07, 0x23, 0x15, 0x66, 0x74 },
            encoded.get()
        );
    }

    @Test
    public void tryEncodeChunkReturnsEmptyForInvalidInput() {
        Optional<byte[]> encoded = M7clNameCodec.tryEncodeChunk("toolong");

        assertTrue(encoded.isEmpty());
    }

    @Test
    public void encodeChunkThrowsDescriptiveErrorForTooLongInput() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> M7clNameCodec.encodeChunk("toolong")
        );

        assertTrue(
            ex.getMessage().contains("at most 4 characters"),
            "Error should describe M7CL chunk length limit"
        );
    }

    @Test
    public void tryEncodeNameReturnsEmptyWhenNameIsTooLongForM7clShortName() {
        Optional<M7clNameCodec.EncodedName> encoded =
            M7clNameCodec.tryEncodeName("Overhead L");

        assertTrue(encoded.isEmpty(),
            "M7CL short name is 8 chars max, so Overhead L should not encode");
    }

    private void assertChunk(String text, byte[] expected) {
        byte[] actual = M7clNameCodec.encodeChunk(text);

        System.out.println(String.format(
            "%s => %s Expected %s",
            text,
            SysexParser.bytesToHex(actual),
            SysexParser.bytesToHex(expected)
        ));

        assertArrayEquals(expected, actual);
        assertEquals(text, M7clNameCodec.decodeChunk(actual).trim());
    }
}