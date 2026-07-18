package MidiControl.Controls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;

import MidiControl.ControlServer.CanonicalInputEvent;
import MidiControl.NrpnUtils.NrpnMapping;
import MidiControl.NrpnUtils.NrpnMappingLoader;
import MidiControl.SysexUtils.RegistryReloadListener;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexParser;

public class CanonicalRegistry implements SourceAllInstances {

    private final Map<String, ControlGroup> groups = new HashMap<>();
    private final Map<String, ControlInstance> controlsById = new HashMap<>();
    private final Map<Integer,
                    Map<String, ControlInstance>>
            contextLookup = new HashMap<>();
    private Map<String, ControlInstance> controlsByNrpn;
    private SysexParser sysexParser;
    private final List<RegistryReloadListener> reloadListeners = new ArrayList<>();
    private String deskType = "";
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
        buildContextIndex();
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

    public String getDeskType(){
        return deskType;
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
                    });
                }
            }
        }
    }

    public void attachNrpnMappings(List<NrpnMapping> nrpnMappings) {
        controlsByNrpn = new HashMap<>();
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
            controlsByNrpn.put(
                nrpn.msbInt() + ":" + nrpn.lsbInt(),
                ci
            );
        }
    }

    private void buildContextIndex() {
        contextLookup.clear();

        for (ControlGroup cg : groups.values()) {
            for (SubControl sc : cg.getSubcontrols().values()) {

                for (ControlInstance ci : sc.getInstances()) {

                    contextLookup
                        .computeIfAbsent(
                            ci.getInstanceIndex(),
                            k -> new HashMap<>())
                        .put(
                            ci.getGroup() + "|" + ci.getSubcontrol(),
                            ci);
                }
            }
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

        if(mapping==null){return null;}
        
        if(debug){logger.info("DEBUG: Mapping matched = " + (mapping == null ? "null" : mapping.getControlGroup() + "." + mapping.getSubControl()));}
        ControlGroup cg = groups.get(mapping.getControlGroup());

        if (cg == null) {
            return null;
        }

        SubControl sc = cg.getSubcontrol(mapping.getSubControl());

        if (sc == null) {
            return null;
        }

        int index = mapping.extractIndex(message);

        if (index < 0 || index >= mapping.getMax_Channels()) {
            return null;
        }

        return sc.getInstances().get(index);
    }

    public ControlInstance resolveNrpn(int msb, int lsb) {
        String key = msb + ":" + lsb;

        ControlInstance ci = controlsByNrpn.get(key);

        if (ci == null) {
            logger.warning(
                String.format("NRPN lookup failed for %s registry size=%d",key, controlsByNrpn.size())
            );
        }

        return ci;
    }

    private ControlInstance resolveCc(ShortMessage cc) {
        return null;
    }

    /**
     * Returns all ControlInstances that belong to a given context.
     * Example contextId: "channel.1", "channel.22"
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
     */
    public int extractContextIndex(String contextId) {
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

    public ControlInstance find(int index,
                                String group,
                                String subcontrol)
    {
        Map<String, ControlInstance> context =
                contextLookup.get(index);

        if (context == null) {
            return null;
        }

        return context.get(group + "|" + subcontrol);
    }


    public void reloadMappings(List<SysexMapping> newMappings, SysexParser newParser, String deskType) {
        this.groups.clear();
        this.controlsById.clear();

        Map<String, ControlGroup> built = ControlFactory.fromSysexMappings(newMappings);
        this.groups.putAll(built);

        this.sysexParser = newParser;
        this.deskType = deskType;

        indexControlsByCanonicalId();
        buildContextIndex();
        if(deskType.isEmpty())logger.warning("Cannot attach NRPN mappings as the deskType is null");
        attachNrpnMappings(NrpnMappingLoader.loadFromResource(MidiControl.NrpnUtils.MappingFiles.getFilePathByKey(deskType)));
        attachBroadcastListeners();
        notifyReloadListeners();

    }

    public void addReloadListener(RegistryReloadListener l) {
        logger.info("Adding reload listener "+l.hashCode());
        reloadListeners.add(l);
    }

    private void notifyReloadListeners() {
        logger.info("Registry reloaded - notifying listeners");
        for (RegistryReloadListener l : reloadListeners) {
            l.onRegistryReloaded(this);
        }
    }

    public void setDeskType(String deskString) {
        this.deskType = deskString;
    }
}