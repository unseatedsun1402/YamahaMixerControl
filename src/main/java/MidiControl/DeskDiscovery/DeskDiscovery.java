package MidiControl.DeskDiscovery;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;
import MidiControl.MidiDeviceManager.MidiDeviceDTO;
import MidiControl.MidiDeviceManager.MidiIOManager;
import MidiControl.Server.Rehydration.RehydrationManager;
import MidiControl.SysexUtils.MappingFiles;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;

public class DeskDiscovery {

    private static final Logger logger = Logger.getLogger(DeskDiscovery.class.getName());

    private MidiIOManager ioManager;
    private RehydrationManager rehydrationManager;
    public JsonArray deskProfiles;
    private Map<String,SysexMapping> deskProfileMap;
    private CanonicalRegistry registry;

    @FunctionalInterface
    public interface ProbeCallback {
        void onProbeSuccess(ControlInstance instance, int midi_channel);
    }

    private boolean isBlacklisted(MidiDeviceDTO dto) {
        if (dto.name == null) return false;
        String n = dto.name.toLowerCase();
        String d = dto.description.toLowerCase();

        boolean result = n.contains("loop") ||
            n.contains("virtual") ||
            n.contains("sequencer") ||
            n.contains("mapper") ||
            n.contains("bluetooth") ||
            d.contains("loop") ||
            d.contains("virtual") ||
            d.contains("sequencer") ||
            d.contains("mapper") ||
            d.contains("bluetooth") ||
            d.contains("gervill");

        if (result) {
            logger.info(String.format("Blacklisted device %s - will be skipped", n));
        }
        return result;
    }

    public DeskDiscovery() {}

    public DeskDiscovery(MidiIOManager ioManager) {
        this.ioManager = ioManager;
        this.rehydrationManager = (ioManager != null ? ioManager.getRehydrationManager() : null);
    }

    public void discover() {
        loadMappingsFromResource("MidiControl/discovery/known-desks.json");

        if (ioManager != null) {
            try {
                ioManager.listDeviceDTOs();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to get Midi Devices for discovery", e);
            }
        }
    }

    public DeskDiscoveryResult discoverDeskModel() {
        logger.info("DeskDiscovery: starting discoverDeskModel()");
        loadMappingsFromResource("MidiControl/discovery/known-desks.json");

        if (ioManager == null) {
            logger.warning("DeskDiscovery: ioManager is null, aborting discovery");
            return null;
        }
        if (registry == null) {
            logger.warning("DeskDiscovery: registry is null, aborting discovery");
            return null;
        }
        if (rehydrationManager == null) {
            logger.warning("DeskDiscovery: rehydrationManager is null, aborting discovery");
            return null;
        }

        final AtomicReference<DeskDiscoveryResult> detected = new AtomicReference<>(null);
        final AtomicBoolean stop = new AtomicBoolean(false);

        List<MidiDeviceDTO> devices = ioManager.listDeviceDTOs();
        logger.info(String.format("DeskDiscovery: found %d MIDI devices",devices.size()));

        for (JsonElement jsonElement : deskProfiles) {
            if (stop.get()) break;

            JsonObject deskProfile = jsonElement.getAsJsonObject();
            String deskModel = getDeskModel(deskProfile);
            logger.info("DeskDiscovery: trying candidate desk model " + deskModel);

            try {
                List<SysexMapping> fullMappings =
                    SysexMappingLoader.loadMappingsFromResource(MappingFiles.getFilePathByKey(deskModel));

                for (SysexMapping m : fullMappings) {
                    m.initialize();
                }

                SysexParser parser = new SysexParser(fullMappings);

                logger.info("DeskDiscovery: reloading registry for candidate desk " + deskModel);
                registry.reloadMappings(fullMappings, parser, deskModel);

                SysexMapping probeMapping = getSysexMapping(deskProfile);
                probeMapping.initialize();

                logger.info("DeskDiscovery: probe mapping group=" +
                    probeMapping.getControlGroup() + " sub=" + probeMapping.getSubControl());

                List<int[]> pairs = new ArrayList<>();

                // Build candidate pairs by matching names
                for (int out = 0; out < devices.size(); out++) {
                    MidiDeviceDTO outDev = devices.get(out);
                    if (!outDev.canOutput) continue;
                    if (isBlacklisted(outDev)) continue;

                    for (int in = 0; in < devices.size(); in++) {
                        MidiDeviceDTO inDev = devices.get(in);
                        if (!inDev.canInput) continue;
                        if (isBlacklisted(inDev)) continue;

                        if (outDev.name != null && outDev.name.equals(inDev.name)) {
                            logger.info(String.format(
                                "DeskDiscovery: candidate pair out=%d (%s) in=%d (%s)",
                                out, outDev.name, in, inDev.name
                            ));
                            pairs.add(new int[]{out, in});
                        }
                    }
                }

                if (pairs.isEmpty() && !devices.isEmpty()) {
                    logger.info("DeskDiscovery: no matching name pairs, using fallback pair [0,0]");
                    pairs.add(new int[]{0, 0});
                }

                for (int[] pair : pairs) {
                    if (stop.get()) break;

                    int out = pair[0];
                    int in = pair[1];

                    MidiDeviceDTO outDev = devices.get(out);
                    if (!outDev.canOutput) {
                        logger.info("DeskDiscovery: outDev " + outDev.name + " cannot output, skipping");
                        continue;
                    }
                    if (isBlacklisted(outDev)) continue;
                    if (!ioManager.trySetOutputDevice(out)) {
                        logger.info("DeskDiscovery: failed to set output device index " + out);
                        continue;
                    }

                    MidiDeviceDTO inDev = devices.get(in);
                    if (isBlacklisted(inDev)) continue;
                    if (!inDev.canInput) {
                        logger.info("DeskDiscovery: inDev " + inDev.name + " cannot input, skipping");
                        continue;
                    }
                    if (!ioManager.trySetInputDevice(in)) {
                        logger.info("DeskDiscovery: failed to set input device index " + in);
                        continue;
                    }

                    if (!ioManager.hasValidDevices()) {
                        logger.info("DeskDiscovery: ioManager reports invalid devices, skipping pair");
                        continue;
                    }

                    for (int channel = 0; channel < 16; channel++) {
                        if (stop.get()) break;

                        DeskDiscoveryResult result =
                            probeWithCanonicalId(deskModel, probeMapping, channel, 100, 110);

                        if (result != null) {
                            detected.set(result);
                            stop.set(true);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                logger.severe("DeskDiscovery: exception while processing candidate desk " + deskModel);
                e.printStackTrace();
            }
        }

        DeskDiscoveryResult result = detected.get();
        if (result == null) {
            logger.info("DeskDiscovery: no desk detected");
        } else {
            logger.info("DeskDiscovery: detected desk " + result.getModel() +
                " on MIDI channel " + result.midiChannel());
        }
        return result;
    }

    public boolean probeForLiveness(String deskModel) {
        SysexMapping mapping = deskProfileMap.get(deskModel);
        mapping = registry.resolveCanonicalId(buildCanonicalIdFromMapping(mapping,0)).getSysex();
        if (mapping == null) return false;

        DeskDiscoveryResult result =
            probeWithCanonicalId(deskModel, mapping, 0, 100,110);

        return result != null;
    }

    private DeskDiscoveryResult probeWithCanonicalId(String deskModel,SysexMapping mapping,int channel,long timeoutMs, long waitForMs) 
    {
        final AtomicReference<DeskDiscoveryResult> detected = new AtomicReference<>(null);
        final CountDownLatch latch = new CountDownLatch(1);

        logger.info("Desk model probing "+deskModel);

        String canonicalId = buildCanonicalIdFromMapping(mapping, channel);

        try {
            logger.info(String.format("Probing canonicalId=%s channel=%d",canonicalId,channel));
            rehydrationManager.probe(
            canonicalId,
            timeoutMs,
            channel,
            (instance, midiChannel) -> {
                if (instance != null && latch.getCount() > 0) {
                    detected.set(new DeskDiscoveryResult(deskModel, midiChannel));
                    latch.countDown();
                }
            }
        );
        } catch (Exception e) {
            logger.log(Level.SEVERE,
                "Probe failed for canonicalId=" + canonicalId,
                e
            );
        }

        try {
            latch.await(waitForMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return detected.get();
    }

    private void loadMappingsFromResource(String resourceName) {
        if (deskProfiles != null) return;

        try (InputStream is =
                 DeskDiscovery.class.getClassLoader().getResourceAsStream(resourceName)) {

            if (is == null) throw new RuntimeException("Resource not found: " + resourceName);

            InputStreamReader reader = new InputStreamReader(is);
            deskProfiles = new Gson().fromJson(reader, JsonArray.class);
            mapDeskProfiles();
            logger.info("DeskDiscovery: loaded " + deskProfiles.size() +
                " desk profiles from " + resourceName);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load mappings from " + resourceName, e);
        }
    }

    private void mapDeskProfiles() {
        if (deskProfileMap == null) {
            deskProfileMap = new HashMap<>();
            logger.info("Building desk profile map");
        }

        if(deskProfiles == null) {
            logger.warning("No desk profiles available - cannot build desk profile map");
            return;
        }

        for (JsonElement profile : deskProfiles) {
            JsonObject profileObject = profile.getAsJsonObject();
            String deskModel = getDeskModel(profileObject);
            SysexMapping mapping = getSysexMapping(profileObject);

            mapping.initialize();
            deskProfileMap.put(deskModel, mapping);
            logger.info(String.format("Desk profile mapp added for %s",deskModel));
        }
    }

    private String getDeskModel(JsonObject profile){
        return profile.get("deskmodel").getAsString();
    }

    private SysexMapping getSysexMapping(JsonObject profile){
        return new Gson().fromJson(
                    profile.get("sysexmapping"),
                    SysexMapping.class
                );
    }

    private String buildCanonicalIdFromMapping(SysexMapping mapping,int index){
        if(mapping == null)logger.info(String.format("Mapping is null"));
        return String.format( "%s.%s.%d",mapping.getControlGroup(),mapping.getSubControl(),index);
    }

    public int getKnownDeskProfilesSize() {
        return deskProfiles.size();
    }

    public void setRehydrationManager(RehydrationManager testManager) {
        this.rehydrationManager = testManager;
    }

    public void injectNewRegistry (CanonicalRegistry registry) {
        this.registry = registry;
        logger.info("Registry injected - @" + registry.hashCode());
    }
}
