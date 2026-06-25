package MidiControl.UserInterface.ChannelName.Codecs;

import java.util.Optional;

public final class M7clNameCodec {

    private M7clNameCodec() {}

    public record EncodedName(byte[] short1, byte[] short2) {}

    public static String decodeChunk(byte[] dd) {
        if (dd == null || dd.length != 5) {
            throw new IllegalArgumentException("M7CL name chunk must be exactly 5 bytes");
        }

        long n = 0;

        for (byte b : dd) {
            n = (n << 7) | (b & 0x7F);
        }

        char c0 = decodeChar((int) ((n >> 24) & 0xFF));
        char c1 = decodeChar((int) ((n >> 16) & 0xFF));
        char c2 = decodeChar((int) ((n >> 8) & 0xFF));
        char c3 = decodeChar((int) (n & 0xFF));

        return "" + c0 + c1 + c2 + c3;
    }

    public static String decodeName(byte[] short1, byte[] short2) {
        return rtrim(decodeChunk(short1) + decodeChunk(short2));
    }

    public static Optional<String> tryDecodeName(byte[] short1, byte[] short2) {
        try {
            String decoded = decodeName(short1, short2);

            if (decoded.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(decoded);
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    public static byte[] encodeChunk(String text) {
        if (text == null) {
            text = "";
        }

        if (text.length() > 4) {
            throw new IllegalArgumentException(
                "M7CL name chunk can contain at most 4 characters"
            );
        }

        byte[] raw = new byte[4];

        for (int i = 0; i < 4; i++) {
            if (i < text.length()) {
                raw[i] = (byte) text.charAt(i);
            } else {
                raw[i] = 0; // Yamaha uses NUL padding, not space padding
            }
        }

        long n =
            ((long) raw[0] & 0xFF) << 24 |
            ((long) raw[1] & 0xFF) << 16 |
            ((long) raw[2] & 0xFF) << 8 |
            ((long) raw[3] & 0xFF);

        return new byte[] {
            (byte) ((n >> 28) & 0x7F),
            (byte) ((n >> 21) & 0x7F),
            (byte) ((n >> 14) & 0x7F),
            (byte) ((n >> 7) & 0x7F),
            (byte) (n & 0x7F)
        };
    }

    public static Optional<byte[]> tryEncodeChunk(String text) {
        try {
            return Optional.of(encodeChunk(text));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    public static EncodedName encodeName(String name) {
        if (name == null) {
            name = "";
        }

        if (name.length() > 8) {
            throw new IllegalArgumentException(
                "M7CL short name can contain at most 8 characters"
            );
        }

        String first = name.length() <= 4
            ? name
            : name.substring(0, 4);

        String second = name.length() <= 4
            ? ""
            : name.substring(4);

        return new EncodedName(
            encodeChunk(first),
            encodeChunk(second)
        );
    }

    public static Optional<EncodedName> tryEncodeName(String name) {
        try {
            return Optional.of(encodeName(name));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private static char decodeChar(int value) {
        if (value == 0) {
            return ' ';
        }

        if (value < 32 || value > 126) {
            return ' ';
        }

        return (char) value;
    }

    private static String rtrim(String value) {
        int last = value.length() - 1;

        while (last >= 0 && value.charAt(last) == ' ') {
            last--;
        }

        if (last < 0) {
            return "";
        }

        return value.substring(0, last + 1);
    }
}