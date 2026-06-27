package MidiControl.UserInterface.ChannelName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import MidiControl.ContextModel.ContextFilter;
import MidiControl.ContextModel.Context;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.ControlInstance;
import MidiControl.Controls.ControlListener;
import MidiControl.Controls.SubControl;
import MidiControl.UserInterface.ChannelName.Codecs.ChannelNameCodec;
import MidiControl.UserInterface.ChannelName.Codecs.Packed32Codec;
import MidiControl.UserInterface.ChannelName.Codecs.PerByteCodec;

public class ChannelNameAssembler implements ControlListener {

    private Context context;
    private CanonicalRegistry registry;
    private ChannelNameListener listener;
    private static Map<String,String> nameCache = new HashMap<>();
    private static String deskLifeName = "";
    private volatile ChannelNameCodec codec;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> pendingTask = null;
    private static final long DEBOUNCE_MS = 800;

    private static final Logger logger = Logger.getLogger(ChannelNameAssembler.class.getName());
    private volatile boolean pendingNameUpdate = false;

    private Map<String,List<ControlInstance>> instanceMap;
    private Map<ControlInstance,String> channelMap;
    private String CanonicalName;

    private static volatile boolean debug = false;

    public static void enableDebug() {
        debug = true;
    }

    public ChannelNameAssembler(Context ctx,
                                CanonicalRegistry registry,
                                ChannelNameListener listener) {
        if (deskLifeName == "") {
            deskLifeName = registry.getDeskType();
            logger.info("Building assemblers for desk " + deskLifeName + " " + registry.hashCode());
        }

        this.codec = selectCodec(registry.getDeskType());
        this.context = ctx;
        this.registry = registry;
        this.listener = listener;

        this.CanonicalName = context.getId();
        this.instanceMap = new HashMap<>();
        this.channelMap  = new HashMap<>();

        if (CanonicalName == null) return;

        subscribeToControls();
    }

    public ChannelNameAssembler(Context ctx, CanonicalRegistry canonicalRegistry) {
        this(ctx, canonicalRegistry, new ChannelNameBroadcaster());
    }

    public ChannelNameAssembler(Context ctx,
                                CanonicalRegistry registry,
                                ChannelNameListener listener,
                                ChannelNameCodec codec) {
        this(ctx, registry, listener);
        this.codec = codec;
    }

    private void subscribeToControls() {

        List<ControlInstance> contextInstances = new ArrayList<>();
        if (CanonicalName == null) return;

        for (ContextFilter filter : context.getFilters()) {

            if (!"kInputChannelName".equals(filter.getControlGroup())
                && !"kNameInputChannel".equals(filter.getControlGroup())) {
                continue;
            }

            ControlGroup cg = registry.getGroup(filter.getControlGroup());
            if (cg == null) continue;

            SubControl sc = cg.getSubcontrol(filter.getSubControl());
            if (sc == null) continue;

            if (sc.getName().contains("Long")) continue;

            int channelIndex = filter.getIndex();

            for (ControlInstance inst : sc.getInstances()) {

                if (inst == null) continue;
                if (inst.getIndex() != channelIndex) continue;

                inst.addListener(this);

                contextInstances.add(inst);
                channelMap.put(inst, CanonicalName);

                logger.fine("ATTACH " + inst.getCanonicalId() + " -> " + CanonicalName);
            }
        }

        instanceMap.put(CanonicalName, contextInstances);
    }

    @Override
    public void onControlChanged(ControlInstance instance, int newValue) {

        String contextId = channelMap.get(instance);

        if(debug)logger.fine("[IN] " + contextId
            + " <- " + instance.getSubcontrol()
            + "." + instance.getInstanceIndex()
            + " = " + newValue);

        pendingNameUpdate = true;

        if (pendingTask != null)
            pendingTask.cancel(false);

        pendingTask = scheduler.schedule(() -> {

            if (!pendingNameUpdate) return;

            pendingNameUpdate = false;

            String assembled = assembleName(instance);
            String ctx = channelMap.get(instance);

            if (assembled == null) {
                logger.info("[SKIP] " + ctx + " null");
                return;
            }

            if (assembled.isBlank()) {
                logger.info("[SKIP] " + ctx + " blank");
                return;
            }

            if(debug)logger.info("[OUT] " + ctx + " = [" + assembled + "]");

            cacheName(ctx, assembled);
            listener.onChannelNameUpdated(ctx, assembled);

        }, DEBOUNCE_MS, TimeUnit.MILLISECONDS);
    }

    private String assembleName(ControlInstance instance) {

        String ctx = channelMap.get(instance);
        if (ctx == null) return "";

        List<ControlInstance> instances = instanceMap.get(ctx);
        if (instances == null || instances.isEmpty()) return "";

        List<Integer> values = extractValues(instances);

        if (debug) logger.info("[AGG] " + ctx + " values=" + values);

        if (codec == null) return "";

        String decoded = codec.decode(values);

        if (debug) logger.info("[DEC] " + ctx + " -> [" + decoded + "]");

        return decoded;
    }

    private List<Integer> extractValues(List<ControlInstance> instances) {

        TreeMap<Integer, Integer> ordered = new TreeMap<>();

        for (ControlInstance ci : instances) {

            String digits = ci.getSubcontrol().replaceAll("\\D+", "");
            if (digits.isEmpty()) continue;

            int pos = Integer.parseInt(digits);
            ordered.put(pos, ci.getValue());
        }

        return new ArrayList<>(ordered.values());
    }

    private ChannelNameCodec selectCodec(String deskType) {
        if ("YAMAHA_M7CL".equals(deskType)) {
            return new Packed32Codec();
        }
        return new PerByteCodec();
    }

    public void shutdown() {
        if (pendingTask != null) {
            pendingTask.cancel(false);
        }
        scheduler.shutdownNow();

        for (List<ControlInstance> list : instanceMap.values()) {
            for (ControlInstance ci : list) {
                ci.removeListener(this);
            }
        }

        if (deskLifeName != "") deskLifeName = "";
    }

    static void cacheName(String context, String name){
        if (nameCache.containsKey(context)){
            if (nameCache.get(context).equals(name)) return;
            nameCache.put(context, name);
            return;
        }
        nameCache.put(context, name);
    }

    public static Map<String,String> getChannelNames(){
        return Map.copyOf(nameCache);
    }

    public void shutdownScheduler(){
        this.scheduler.shutdownNow();
    }
}