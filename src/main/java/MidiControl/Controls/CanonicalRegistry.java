package MidiControl.Controls;

import MidiControl.ControlServer.CanonicalInputEvent;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexParser;
import MidiControl.NrpnUtils.NrpnMapping;

import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CanonicalRegistry implements SourceAllInstances {

    private final Map<String, ControlGroup> groups = new HashMap<>();
    private final Map<String, ControlInstance> controlsById = new HashMap<>();
    private final SysexParser sysexParser;
    private static final Logger logger = Logger.getLogger(CanonicalRegistry.class.getName());
    private boolean debug = false;

    public void enableDebug() {
        this.debug = true;
    }

    public CanonicalRegistry(List<SysexMapping> sysexMappings,SysexParser sysexParser) {
        Map<String, ControlGroup> built = ControlFactory.fromSysexMappings(sysexMappings);
        this.sysexParser = sysexParser;
        groups.putAll(built);
        indexControlsByCanonicalId();
        attachBroadcastListeners();
    }

    private void indexControlsByCanonicalId() {
        for (ControlGroup cg : groups.values()) {
            for (SubControl sc : cg.getSubcontrols().values()) {
                for (ControlInstance ci : sc.getInstances()) {
                    controlsById.put(ci.getCanonicalId(), ci);
                }
            }
        }
    }

    public ControlGroup getGroup(String name) {
        return groups.get(name);
    }

    public Map<String, ControlGroup> getGroups() {
        return groups;
    }

    public ControlInstance resolveCanonicalId(String canonicalId) {
        return controlsById.get(canonicalId);
    }

    private void attachBroadcastListeners() {
        for (ControlGroup cg : groups.values()) {
            for (SubControl sc : cg.getSubcontrols().values()) {
                for (ControlInstance ci : sc.getInstances()) {
                    ci.addListener((instance, newValue) -> {
                        // String json = buildDualFormatJson(instance, newValue);
                        // WebSocketEndpoint.broadcast(json);
                    });
                }
            }
        }
    }

    // private String buildDualFormatJson(ControlInstance instance, int value) {
    //     var nrpn = instance.getNrpn().orElse(null);
    //     int msb = nrpn != null ? nrpn.msbInt() : -1;
    //     int lsb = nrpn != null ? nrpn.lsbInt() : -1;

    //     return String.format(
    //         "{\"canonicalId\":\"%s\",\"value\":%d,\"msb\":%d,\"lsb\":%d}",
    //         instance.getCanonicalId(),
    //         value,
    //         msb,
    //         lsb
    //     );
    // }

    public void attachNrpnMappings(List<NrpnMapping> nrpnMappings) {
        for (NrpnMapping nrpn : nrpnMappings) {
            String[] parts = nrpn.getCanonicalId().split("\\.");
            if (parts.length != 3) continue;

            String group = parts[0];
            String sub   = parts[1];
            int instance = Integer.parseInt(parts[2]);

            ControlGroup cg = groups.get(group);
            if (cg == null) continue;

            SubControl sc = cg.getSubcontrol(sub);
            if (sc == null) continue;

            if (instance >= sc.getInstances().size()){
                logger.warning("Failed to attach nrpn: " + nrpn.getCanonicalId() + ": Out of index");
                continue;
            }
            ControlInstance ci = sc.getInstances().get(instance);
            if (ci == null) continue;

            ci.setNrpn(nrpn);
        }
    }

    public ControlInstance resolve(CanonicalInputEvent event) throws MidiUnavailableException {
        return switch (event.getType()) {
            case SYSEX -> resolveSysex(event.getSysexData());
            case NRPN  -> resolveNrpn(event.getNrpn().msb, event.getNrpn().lsb);
            case CC    -> resolveCc(event.getCc());
        };
    }

    public ControlInstance resolveSysex(byte[] message) throws MidiUnavailableException {

        SysexMapping mapping = sysexParser.processMidiMessage(message);
        System.out.println("DEBUG: Mapping matched = " + (mapping == null ? "null" : mapping.getControlGroup() + "." + mapping.getSubControl()));

        if (mapping == null) {
            return null;
        }

        ControlGroup cg = groups.get(mapping.getControlGroup());

        if (cg == null) {
            return null;
        }

        SubControl sc = cg.getSubcontrol(mapping.getSubControl());

        if (sc == null) {
            return null;
        }

        int index = mapping.extractIndex(message);

        // BEFORE the get() call:
        if (index < 0 || index >= sc.getInstances().size()) {
            // Let the test throw naturally:
            return sc.getInstances().get(index); // this will throw
        }

        ControlInstance ci = sc.getInstances().get(index);

        return ci;
    }

    public ControlInstance resolveNrpn(int msb, int lsb) {
        for (ControlInstance ci : controlsById.values()) {
            if (ci.getNrpn().isPresent()) {
                NrpnMapping nrpn = ci.getNrpn().get();
                if (nrpn.msbInt() == msb && nrpn.lsbInt() == lsb) {
                    return ci;
                }
            }
        }
        return null;
    }

    private ControlInstance resolveCc(ShortMessage cc) {
        return null;
    }

    /**
     * Returns all ControlInstances that belong to a given context.
     * Example contextId: "channel.1", "channel.22"
     *
     * This method extracts the instance index from the context ID
     * and returns every ControlInstance whose SubControl has an
     * instance at that index.
     */
    public List<ControlInstance> getAllInstancesForContext(String contextId) {
        List<ControlInstance> result = new ArrayList<>();

        int index = extractContextIndex(contextId);
        if (index < 0)
            return result;

        for (ControlGroup cg : groups.values()) {
            for (SubControl sc : cg.getSubcontrols().values()) {
                List<ControlInstance> instances = sc.getInstances();
                if (index < instances.size()) {
                    ControlInstance ci = instances.get(index);
                    if (ci != null) {
                        result.add(ci);
                    }
                }
            }
        }

        return result;
    }

    @Override
    public Collection<ControlInstance> getAllInstances() {
        return groups.values().stream()
            .flatMap(cg -> cg.getSubcontrols().values().stream())
            .flatMap(sc -> sc.getInstances().stream())
            .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Extracts the numeric instance index from a context ID.
     * Examples:
     *   "channel.1"   → 1
     *   "mix.12"      → 12
     *   "matrix.3-4"  → 3
     */
    private int extractContextIndex(String contextId) {
        int dot = contextId.lastIndexOf('.');
        if (dot == -1)
            return -1;

        String tail = contextId.substring(dot + 1);

        // Handle ranges like "3-4"
        if (tail.contains("-")) {
            tail = tail.split("-")[0];
        }

        try {
            return Integer.parseInt(tail);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

public ControlInstance resolve(String string) {
    if(!string.matches("^[A-Za-z0-9_]+\\.[A-Za-z0-9_]+\\.[0-9]+$")){return null;}
    int index = Integer.parseInt(string.split("\\.")[2]);
    if (index < 0)
        return null;

    ControlGroup group = this.getGroup(string.split("\\.")[0]);
    SubControl subControl = group.getSubcontrol(string.split("\\.")[1]);
    if(index < subControl.getInstances().size()){return subControl.getInstances().get(index);}
    return null;
}
}