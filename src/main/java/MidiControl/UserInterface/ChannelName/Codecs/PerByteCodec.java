package MidiControl.UserInterface.ChannelName.Codecs;

import java.util.List;
import java.util.Optional;

public class PerByteCodec implements ChannelNameCodec {

    @Override
    public String decode(List<Integer> values) {

        StringBuilder sb = new StringBuilder();

        for (int raw : values) {
            int v = raw & 0x7F;
            if (v >= 32 && v <= 126) {
                sb.append((char) v);
            } else {
                sb.append(' ');
            }
        }

        return rtrim(sb.toString());
    }

    private String rtrim(String s) {
        int last = s.length() - 1;
        while (last >= 0 && s.charAt(last) == ' ') last--;
        return last < 0 ? "" : s.substring(0, last + 1);
    }

    @Override
    public Optional<List<byte[]>> encode(String name) {
        return Optional.of(
            name.chars()
                .mapToObj(c -> new byte[]{ (byte) c })
                .toList()
        );
    }
}