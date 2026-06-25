package MidiControl.unit.UserInterface.Codec;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import MidiControl.UserInterface.ChannelName.Codecs.Packed32Codec;

public class Packed32CodecTest {

    private final Packed32Codec codec = new Packed32Codec();

    // ---------------------------------------------------------
    // Decode tests
    // ---------------------------------------------------------

    @Test
    public void decodesPacked32ValuesIntoFourCharacters() {

        // "test" = 0x74657374
        int packed = 0x74657374;

        String result = codec.decode(List.of(packed));

        assertEquals("test", result);
    }

    @Test
    public void decodesMultiplePackedChunksInOrder() {

        // "SPX " = 0x53505820
        // "MIDI" = 0x4D494449
        int chunk1 = 0x53505820;
        int chunk2 = 0x4D494449;

        String result = codec.decode(List.of(chunk1, chunk2));

        assertEquals("SPX MIDI", result);
    }

    @Test
    public void trimsTrailingSpacesFromPackedValues() {

        // "AAA " = 0x41414120
        int packed = 0x41414120;

        String result = codec.decode(List.of(packed));

        assertEquals("AAA", result);
    }

    @Test
    public void replacesNullBytesWithSpaces() {

        // "A\0\0\0" = 0x41000000
        int packed = 0x41000000;

        String result = codec.decode(List.of(packed));

        assertEquals("A", result); // trailing spaces trimmed
    }

    @Test
    public void ignoresZeroOrNegativeValues() {

        String result = codec.decode(List.of(0, -1));

        assertEquals("", result);
    }

    @Test
    public void mixedPackedAndAsciiValuesStillDecode() {

        int packed = 0x74657374; // "test"

        String result = codec.decode(List.of(packed, (int) '1'));

        assertEquals("test1", result);
    }

    // ---------------------------------------------------------
    // Encode tests
    // ---------------------------------------------------------

    @Test
    public void encodesEightCharacterNameIntoTwoChunks() {

        Optional<List<byte[]>> encoded =
            codec.encode("SPX MIDI");

        assertTrue(encoded.isPresent());
        assertEquals(2, encoded.get().size());

        // Just sanity-check sizes (exact byte values already tested elsewhere)
        assertEquals(5, encoded.get().get(0).length);
        assertEquals(5, encoded.get().get(1).length);
    }

    @Test
    public void encodesShortNamesIntoTwoChunksWithPadding() {

        Optional<List<byte[]>> encoded =
            codec.encode("test1");

        assertTrue(encoded.isPresent());
        assertEquals(2, encoded.get().size());
    }

    @Test
    public void returnsEmptyOptionalWhenNameTooLong() {

        Optional<List<byte[]>> encoded =
            codec.encode("Overhead L"); // 10 chars

        assertTrue(encoded.isEmpty());
    }
}
