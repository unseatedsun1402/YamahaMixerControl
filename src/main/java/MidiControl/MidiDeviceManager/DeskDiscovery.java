package MidiControl.MidiDeviceManager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
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
        logger.info("DeskDiscovery: found " + devices.size() + " MIDI devices");

        for (JsonElement jsonElement : deskProfiles) {
            if (stop.get()) break;

            JsonObject deskProfile = jsonElement.getAsJsonObject();
            String deskModel = deskProfile.get("deskmodel").getAsString();
            logger.info("DeskDiscovery: trying candidate desk model " + deskModel);

            try {
                List<SysexMapping> fullMappings =
                    SysexMappingLoader.loadMappingsFromResource(MappingFiles.getFilePathByKey(deskModel));

                for (SysexMapping m : fullMappings) {
                    m.initialize();
                }

                SysexParser parser = new SysexParser(fullMappings);

                // 2. Reload the global registry for this candidate desk
                logger.info("DeskDiscovery: reloading registry for candidate desk " + deskModel);
                registry.reloadMappings(fullMappings, parser, deskModel);

                // 3. Use the discovery-specific mapping from known-desks.json to choose which control to probe
                SysexMapping probeMapping =
                    new Gson().fromJson(deskProfile.get("sysexmapping"), SysexMapping.class);
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

                // Fallback for tests / degenerate cases: no pairs → single pseudo pair
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

                    CountDownLatch batchLatch = new CountDownLatch(1);

                    // Fire all 16 probes asynchronously
                    for (int channel = 0; channel < 16; channel++) {
                        if (stop.get()) break;
                        final int probeChannel = channel;

                        String canonicalId = String.format(
                            "%s.%s.%d",
                            probeMapping.getControlGroup(),
                            probeMapping.getSubControl(),
                            channel
                        );

                        logger.info(String.format(
                            "DeskDiscovery: probing canonicalId=%s channel=%d",
                            canonicalId, channel
                        ));

                        try {
                            rehydrationManager.probe(
                                canonicalId,
                                100,
                                channel,
                                (instance, midiChannel) -> {
                                    logger.info(String.format(
                                        "DeskDiscovery: probe callback for canonicalId=%s channel=%d instance=%s midiChannel=%d",
                                        canonicalId,
                                        probeChannel,
                                        (instance == null ? "null" : instance.getCanonicalId()),
                                        midiChannel
                                    ));

                                    if (instance != null && !stop.get()) {
                                        detected.set(new DeskDiscoveryResult(
                                            deskModel,
                                            midiChannel
                                        ));
                                        stop.set(true);
                                        batchLatch.countDown();
                                    }
                                }
                            );
                        } catch (Exception e) {
                            logger.log(Level.SEVERE,
                                "DeskDiscovery: probe threw exception for canonicalId=" + canonicalId,
                                e);
                        }
                    }

                    // Wait for either success or all timeouts
                    try {
                        batchLatch.await(200, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.log(Level.WARNING, "DeskDiscovery: batchLatch interrupted", e);
                    }
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE,
                    "DeskDiscovery: exception while processing candidate desk " + deskModel,
                    e);
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

    private void loadMappingsFromResource(String resourceName) {
        if (deskProfiles != null) return;

        try (InputStream is =
                 DeskDiscovery.class.getClassLoader().getResourceAsStream(resourceName)) {

            if (is == null) throw new RuntimeException("Resource not found: " + resourceName);

            InputStreamReader reader = new InputStreamReader(is);
            deskProfiles = new Gson().fromJson(reader, JsonArray.class);
            logger.info("DeskDiscovery: loaded " + deskProfiles.size() +
                " desk profiles from " + resourceName);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load mappings from " + resourceName, e);
        }
    }

    public int getKnownDeskProfilesSize() {
        return deskProfiles.size();
    }

    public void setRehydrationManager(RehydrationManager testManager) {
        this.rehydrationManager = testManager;
    }

    public void setDiscoveryRegistry(CanonicalRegistry registry) {
        this.registry = registry;
        logger.info("Registry injected - " + registry);
    }
}
