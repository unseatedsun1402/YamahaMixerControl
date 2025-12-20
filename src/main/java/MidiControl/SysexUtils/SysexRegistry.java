package MidiControl.SysexUtils;

import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SysexRegistry {
    private final Logger log = Logger.getLogger(this.getClass().getName());
    private final List<SysexMapping> mappings;

    // model → key → list of mappings
    private final Map<Integer, Map<Long, List<SysexMapping>>> modelTables = new HashMap<>();

    public SysexRegistry(List<SysexMapping> mappings) {
        this.mappings = mappings;
        buildModelTables(mappings);
    }

    private void buildModelTables(List<SysexMapping> mappings) {
        for (SysexMapping m : mappings) {
            Long key = m.getKey(); // consume precomputed key directly
            if (key == null) continue;

            int model = (int) (key >>> 32); // top 8 bits = model_id

            modelTables
                .computeIfAbsent(model, k -> new HashMap<>())
                .computeIfAbsent(key, k -> new ArrayList<>())
                .add(m);
        }
    }

    public List<SysexMapping> getMappings() {
        return mappings;
    }

    public SysexMapping resolve(byte[] msg) {
        if (msg.length < 10) return null;
        if (msg[0] != (byte)0xF0) return null;
        if (msg[1] != (byte)0x43) return null;
        if (msg[msg.length - 1] != (byte)0xF7) return null;

        // Build key from incoming message bytes 3–7
        int model        = msg[3] & 0xFF;
        int scope        = msg[4] & 0xFF;
        int controlGroup = msg[5] & 0xFF;
        int subControl   = msg[6] & 0xFF;
        int param        = msg[7] & 0xFF;

        long key = ((long) model << 32)
                 | ((long) scope << 24)
                 | ((long) controlGroup << 16)
                 | ((long) subControl << 8)
                 | (long) param;

        Map<Long, List<SysexMapping>> table = modelTables.get(model);
        if (table == null) return null;

        List<SysexMapping> candidates = table.get(key);
        if (candidates == null || candidates.isEmpty()) return null;

        // Return the first mapping for this control type
        return candidates.get(0);
    }
}