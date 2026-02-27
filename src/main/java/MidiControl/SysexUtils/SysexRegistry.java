package MidiControl.SysexUtils;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import MidiControl.SystemTools.NativeLoader;

/**
 * Restored legacy–compatible SysEx registry:
 *
 *  ✓ Global addressBytes per registry (first mapping wins)
 *  ✓ Per‑mapping indexBytes
 *  ✓ No mixed‑desk restriction
 *  ✓ Fully backward‑compatible with existing CanonicalRegistry/SysexParser tests
 *  ✓ Fixes M7CL mapping resolution
 */
public class SysexRegistry {

    private final Logger logger = Logger.getLogger(this.getClass().getName());
    private static final boolean NATIVE = NativeLoader.loadLibrary("native_sysex");

    /** Legacy behaviour: global address bytes chosen from FIRST mapping with metadata */
    private final int[] addressBytes;

    private final List<SysexMapping> mappings;
    private static boolean DEBUG = false;

    public static void enableDebug() { DEBUG = true; }

    public SysexRegistry(List<SysexMapping> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            throw new IllegalStateException("SysexRegistry initialized with no mappings");
        }

        this.mappings = mappings;

        // Restore legacy behaviour: determine ONCE from first mapping with address_bytes
        this.addressBytes = determineAddressBytes(mappings);

        // Native lookup initialisation
        pushToNative(mappings, addressBytes);
    }

    public List<SysexMapping> getMappings() {
        return mappings;
    }

    /**
     * Legacy behaviour: pick the FIRST mapping that defines address_bytes.
     * No mixed‑desk enforcement.
     */
    private int[] determineAddressBytes(List<SysexMapping> mappings) {
        for (SysexMapping m : mappings) {
            int[] addr = m.getAddressBytes();
            if (addr != null && addr.length > 0) {
                return Arrays.copyOf(addr, addr.length);
            }
        }
        throw new IllegalStateException("No address_bytes metadata found in any mapping");
    }

    /**
     * Main Java resolver (slow path).
     * Global addressBytes for key, per‑mapping indexBytes for index.
     */
    public SysexMapping resolve(byte[] msg) {
        if (!isValidSysEx(msg)) return null;

        long key = computeKey(msg);

        if (DEBUG) logger.fine("Computed key=" + key);

        for (SysexMapping m : mappings) {
            if (key == m.getKey()) {
                int index = computeIndex(msg, m);
                if (index != -1) {
                    return m;
                }
            }
        }
        return null;
    }

    /**
     * Legacy key computation:
     * Global registry.addressBytes used for all mappings.
     */
    private long computeKey(byte[] msg) {
        long key = 0;
        for (int b : addressBytes) {
            if (b >= msg.length) return -1;
            key = (key << 8) | (msg[b] & 0x7F);
        }
        return key;
    }

    /**
     * Index computation is still per‑mapping (correct for both desks).
     */
    private int computeIndex(byte[] msg, SysexMapping m) {
        int index = 0;
        for (int b : m.getIndexBytes()) {
            if (b >= msg.length) return -1;
            index = (index << 7) | (msg[b] & 0x7F);
        }
        return (index <= m.getMax_Channels()) ? index : -1;
    }

    /**
     * Native fast resolver fallback.
     */
    public SysexMapping resolveFast(byte[] msg) {
        if (!NATIVE) return resolve(msg);

        int idx = NativeSysex.resolve(msg);
        if (idx < 0 || idx >= mappings.size()) return null;

        return mappings.get(idx);
    }

    /**
     * Push keys/indexes + global address bytes to native lookup table.
     */
    private void pushToNative(List<SysexMapping> mappings, int[] addressBytes) {
        if (!NATIVE) return;

        long[] keys = new long[mappings.size()];
        int[] indexes = new int[mappings.size()];

        for (int i = 0; i < mappings.size(); i++) {
            keys[i]  = mappings.get(i).getKey();
            indexes[i] = i;
        }

        // Native module supports global addressBytes; correct for legacy
        NativeSysex.updateMappings(keys, indexes, addressBytes);
    }

    private boolean isValidSysEx(byte[] msg) {
        if (msg == null || msg.length < 4) return false;
        if (msg[0] != (byte)0xF0) return false;
        if (msg[msg.length - 1] != (byte)0xF7) return false;
        if (msg[1] != (byte)0x43) return false; // Yamaha manufacturer ID
        return true;
    }
}