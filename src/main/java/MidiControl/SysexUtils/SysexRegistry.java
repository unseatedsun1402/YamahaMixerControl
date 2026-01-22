package MidiControl.SysexUtils;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class SysexRegistry {

    private final Logger logger = Logger.getLogger(this.getClass().getName());

    private final List<SysexMapping> mappings;
    private final int[] addressBytes;

    public SysexRegistry(List<SysexMapping> mappings) {
        this.mappings = mappings;
        this.addressBytes = determineAddressBytes(mappings);
        pushToNative(mappings, addressBytes);
    }

    public List<SysexMapping> getMappings() {
        return mappings;
    }

    // Determine global address_bytes for this desk
    // Handles nulls and enforces consistency
    private int[] determineAddressBytes(List<SysexMapping> mappings) {
        int[] first = null;

        for (SysexMapping m : mappings) {
            int[] addr = m.getAddressBytes();

            // Skip mappings that don't define address_bytes (e.g. synthetic)
            if (addr == null || addr.length == 0) {
                continue;
            }

            if (first == null) {
                first = addr;
            } else if (!Arrays.equals(first, addr)) {
                throw new IllegalStateException(
                    "Mixed-desk mappings detected: address_bytes differ between mappings"
                );
            }
        }

        if (first == null) {
            throw new IllegalStateException(
                "No address_bytes metadata found in any mapping"
            );
        }

        return first;
    }

    // Java slow resolver (metadata-driven)
    public SysexMapping resolve(byte[] msg) {
        if (msg.length < 10) return null;
        if (msg[0] != (byte) 0xF0) return null;
        if (msg[1] != (byte) 0x43) return null;
        if (msg[msg.length - 1] != (byte) 0xF7) return null;

        long key = computeKey(msg);
        logger.fine("Computed key: "+Long.toString(key));

        for (SysexMapping m : mappings) {
            if (key == m.getKey()) {
                if (computeIndex(msg, m) != -1) {
                return m;
                }
            }
        }

        return null;
    }

    private long computeKey(byte[] msg) {
        long key = 0;
        for (int b : addressBytes) {
            key = (key << 8) | (msg[b] & 0x7F);
        }
        return key;
    }

    private int computeIndex(byte[] msg, SysexMapping m) {
        int index = 0;
        for (int b : m.getIndexBytes()) {
            index = (index << 7) | (msg[b] & 0x7F);
        }
        if (index <= m.getMax_Channels()){return index;}
        return -1;
    }

    // Native fast resolver
    public SysexMapping resolveFast(byte[] msg) {
        if (!NativeSysex.isNativeAvailable()) {
            logger.fine("Native C method unavailable, falling back to Java resolver");
            return null;
        }

        int index = NativeSysex.resolve(msg);
        if (index < 0 || index >= mappings.size()) {
            return null;
        }

        return mappings.get(index);
    }

    // Push metadata + lookup table to native resolver
    private void pushToNative(List<SysexMapping> mappings, int[] addressBytes) {
        if (!NativeSysex.isNativeAvailable()) {
            logger.warning("Native C method unavailable, cannot update lookup");
            return;
        }

        long[] keys = new long[mappings.size()];
        int[] indexes = new int[mappings.size()];

        for (int i = 0; i < mappings.size(); i++) {
            keys[i] = mappings.get(i).getKey();
            indexes[i] = i;
        }

        NativeSysex.updateMappings(keys, indexes, addressBytes);
    }
}