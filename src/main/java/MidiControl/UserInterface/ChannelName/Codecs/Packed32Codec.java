package MidiControl.UserInterface.ChannelName.Codecs;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Packed32Codec implements ChannelNameCodec {

    @Override
    public String decode(List<Integer> values) {

        if (values.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();

        for (int v : values) {

            if (v <= 0) continue;

            if (v > 127) {
                sb.append(decodePacked32(v));
            }
            else {
                sb.append((char) v);
            }
        }

        return rtrim(sb.toString());
    }

    private String decodePacked32(int n) {

        char c0 = decodeChar((n >> 24) & 0xFF);
        char c1 = decodeChar((n >> 16) & 0xFF);
        char c2 = decodeChar((n >> 8)  & 0xFF);
        char c3 = decodeChar(n & 0xFF);

        return "" + c0 + c1 + c2 + c3;
    }

    private char decodeChar(int v) {
        if (v == 0) return ' ';
        if (v < 32 || v > 126) return ' ';
        return (char) v;
    }

    private String rtrim(String s) {
        int last = s.length() - 1;
        while (last >= 0 && s.charAt(last) == ' ') last--;
        return last < 0 ? "" : s.substring(0, last + 1);
    }

    @Override
    public Optional<List<byte[]>> encode(String name) {

        if (name.length() > 8) {
            return Optional.empty();
        }

        List<byte[]> out = new ArrayList<>();

        String first  = name.substring(0, Math.min(4, name.length()));
        String second = name.length() > 4 ? name.substring(4) : "";

        out.add(M7clNameCodec.encodeChunk(first));
        out.add(M7clNameCodec.encodeChunk(second));

        return Optional.of(out);
    }
}