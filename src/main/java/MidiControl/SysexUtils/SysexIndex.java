package MidiControl.SysexUtils;
import java.util.*;
import MidiControl.SysexUtils.SysexMapping;

public class SysexIndex {
    private final Map<Integer, List<SysexMapping>> byControlId = new HashMap<>();

    public SysexIndex(List<SysexMapping> allMappings) {
        for (SysexMapping m : allMappings) {
            byte controlId = m.getParameter_change_format().get(7) instanceof Number
                    ? ((Number) m.getParameter_change_format().get(7)).byteValue()
                    : -1;
            byControlId
                .computeIfAbsent((int) controlId, k -> new ArrayList<>())
                .add(m);
        }
    }

    public List<SysexMapping> getCandidates(int controlId) {
        return byControlId.getOrDefault(controlId, Collections.emptyList());
    }
}