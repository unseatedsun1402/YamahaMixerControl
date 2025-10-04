package MidiControl.Utilities;

import java.util.List;
import java.util.Arrays;

public class SysExParser {

    private final List<SysExMapping> mappings;

    public SysExParser(List<SysExMapping> mappings) {
        this.mappings = mappings;
    }

    public void parse(byte[] data) {
    for (SysExMapping mapping : mappings) {
        if (matchesPrefix(data, mapping.addressBytes())) {
            int value = extractValue(data, mapping.valueLength());
            apply(mapping, value);
            return;
        }
    }
    System.out.println("Unknown SysEx message: " + Arrays.toString(data));
    }

    private boolean matchesPrefix(byte[] data, byte[] prefix) {
    if (data.length < prefix.length) return false;
    for (int i = 0; i < prefix.length; i++) {
        if (data[i] != prefix[i]) return false;
    }
    return true;
    }

    private int extractValue(byte[] data, int length) {
    int start = data.length - length;
    int value = 0;
    for (int i = 0; i < length; i++) {
        value = (value << 7) | (data[start + i] & 0x7F);
    }
    return value;
    }

    private void apply(SysExMapping mapping, int value) {
    System.out.printf("SYSEX → %s %d → %s = %d%n",
        mapping.sourceChannelType(), mapping.sourceChannelIndex() + 1,
        mapping.controlType(), value);
    }
}
