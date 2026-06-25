package MidiControl.UserInterface.ChannelName.Codecs;

import java.util.List;
import java.util.Optional;


public interface ChannelNameCodec {

    Optional<List<byte[]>> encode(String name);

    String decode(List<Integer> values);
}
