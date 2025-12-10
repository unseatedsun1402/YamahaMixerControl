package MidiControl.Controls;

import MidiControl.SysexUtils.SysexRegistry;
import MidiControl.SysexUtils.SysexMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import MidiControl.NrpnUtils.NrpnMapping;

public class ControlRegistry {
    private final SysexRegistry sysexRegistry;
    private final Map<Integer, Control> sysexControls = new HashMap<>();
    private final Map<Integer, Control> nrpnControls = new HashMap<>();

    public ControlRegistry(List<SysexMapping> sysexMappings, List<NrpnMapping> nrpnMappings) {
        this.sysexRegistry = new SysexRegistry(sysexMappings);

        // Build SysEx index
        for (SysexMapping sysexMapping : sysexMappings) {
            int controlId = sysexMapping.getControl_id();
            Control c = new Control(controlId, sysexMapping, null);
            sysexControls.put(controlId, c);
        }

        // Build NRPN index
        for (NrpnMapping nrpnMapping : nrpnMappings) {
            int nrpnKey = (nrpnMapping.msb() << 7) | nrpnMapping.lsb();
            Control c = new Control(nrpnKey, null, nrpnMapping);
            nrpnControls.put(nrpnKey, c);
        }
    }

    public Control resolve(byte[] message) {
        if (isSysEx(message)) {
            SysexMapping mapping = sysexRegistry.resolve(message);
            if (mapping != null) {
                return sysexControls.get(mapping.getControlId());
            }
        } else if (isNrpn(message)) {
            int nrpnKey = extractNrpnKey(message);
            return nrpnControls.get(nrpnKey);
        }
        return null;
    }
}
