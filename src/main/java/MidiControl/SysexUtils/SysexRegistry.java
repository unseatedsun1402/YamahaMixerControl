package MidiControl.SysexUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SysexRegistry {

    private final Logger logger = Logger.getLogger(this.getClass().getName());

    private final List<SysexMapping> mappings;
    private final Map<Integer, Map<Long, List<SysexMapping>>> modelTables = new HashMap<>();

    public SysexRegistry(List<SysexMapping> mappings) {
        this.mappings = mappings;
        buildModelTables(mappings);
        pushToNative(mappings);
    }

    public record NativeSysexEntry(long key, int index) {}

    private void buildModelTables(List<SysexMapping> mappings) {
        for (SysexMapping m : mappings) {
            long key = m.getKey();
            int model = (int) (key >>> 32);

            modelTables
                .computeIfAbsent(model, k -> new HashMap<>())
                .computeIfAbsent(key, k -> new ArrayList<>())
                .add(m);
        }
    }

    public List<SysexMapping> getMappings() {
        return mappings;
    }

    /**
     * Java-only resolver
     */
    public SysexMapping resolve(byte[] msg) {
        if (msg.length < 10) return null;
        if (msg[0] != (byte) 0xF0) return null;
        if (msg[1] != (byte) 0x43) return null;
        if (msg[msg.length - 1] != (byte) 0xF7) return null;

        int model        = msg[3] & 0xFF;
        int scope        = msg[4] & 0xFF;
        int controlGroup = msg[5] & 0xFF;
        int subControl   = msg[6] & 0xFF;
        int param        = msg[7] & 0xFF;

        long key =
            ((long) model << 32) |
            ((long) scope << 24) |
            ((long) controlGroup << 16) |
            ((long) subControl << 8) |
            (long) param;

        Map<Long, List<SysexMapping>> table = modelTables.get(model);
        if (table == null) return null;

        List<SysexMapping> candidates = table.get(key);
        if (candidates == null || candidates.isEmpty()) return null;

        return candidates.get(0);
    }

    public SysexMapping resolveFast(byte[] msg) {
        if (!NativeSysex.isNativeAvailable()) {
            logger.warning("Native C method is unavailable for Sysex Resolve");
            return null;
        }

        int index = NativeSysex.resolve(msg);
        if (index < 0 || index >= mappings.size()) {
            return null;
        }

        return mappings.get(index);
    }

    private void pushToNative(List<SysexMapping> mappings) {
        if (!NativeSysex.isNativeAvailable()) {
            logger.warning("Native C method is unavailable for to update lookup");
            return;
        }

        long[] keys = new long[mappings.size()];
        int[] indexes = new int[mappings.size()];

        for (int i = 0; i < mappings.size(); i++) {
            keys[i] = mappings.get(i).getKey();
            indexes[i] = i;
        }

        NativeSysex.updateMappings(keys, indexes);
    }
}