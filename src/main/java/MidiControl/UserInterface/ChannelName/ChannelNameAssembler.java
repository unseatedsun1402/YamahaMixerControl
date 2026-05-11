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

public class ChannelNameAssembler implements ControlListener {

    private Context context;
    private CanonicalRegistry registry;
    private ChannelNameListener listener;
    private static Map<String,String> nameCache = new HashMap<>();
    private static String deskLifeName = "";

    // Debounce timer
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> pendingTask = null;
    private static final long DEBOUNCE_MS = 800;
    private static final Logger logger = Logger.getLogger(ChannelNameAssembler.class.getName());
    private volatile boolean pendingNameUpdate = false;
    private volatile static boolean debug = false;
    private Map<String,List<ControlInstance>> instanceMap;
    private Map<ControlInstance,String> channelMap;
    private String CanonicalName;
    

    // passed listener
    public ChannelNameAssembler(Context ctx,
                                CanonicalRegistry registry,
                                ChannelNameListener listener) {
        if (deskLifeName == "") {
            deskLifeName = registry.getDeskType();
            logger.info("Building assemblers for desk "+ deskLifeName+
            " "+registry.hashCode());
        }
        this.context = ctx;
        this.registry = registry;
        this.listener = listener;

        this.CanonicalName = context.getId();

        this.instanceMap = new HashMap<>();
        this.channelMap  = new HashMap<>();

        if(CanonicalName == null) {logger.warning("Context given is null"); return;}

        subscribeToControls();
    }

    // class listener
    public ChannelNameAssembler(Context ctx, CanonicalRegistry canonicalRegistry) {
        this.context = ctx;
        this.registry = canonicalRegistry;
        this.listener = new ChannelNameBroadcaster();
        if (deskLifeName == "") {
            deskLifeName = registry.getDeskType();
            logger.info("Building assemblers for desk "+ deskLifeName+
            " "+registry.hashCode());
        }
        
        this.instanceMap = new HashMap<>();
        this.channelMap  = new HashMap<>();

        this.CanonicalName = context.getId();
        if(CanonicalName == null) {logger.warning("Context given is null"); return;}
        subscribeToControls();
    }

    public static void enableDebug(){
        debug = true;
    }


    private void subscribeToControls() {
        List<ControlInstance> contextInstances = new ArrayList<>();
        if(CanonicalName == null) {logger.warning("Context given is null"); return;}
        for (ContextFilter filter : context.getFilters()) {

            if (!"kInputChannelName".equals(filter.getControlGroup()))
                continue;

            ControlGroup cg = registry.getGroup(filter.getControlGroup());
            if (cg == null) continue;

            SubControl sc = cg.getSubcontrol(filter.getSubControl());
            if (sc == null) continue;

            if (sc.getName().contains("Long")) continue;

            int channelIndex = filter.getIndex();

            for (ControlInstance inst : sc.getInstances()) {

                if (inst.getIndex() != channelIndex)
                    continue;

                inst.addListener(this);
                contextInstances.add(inst);
                channelMap.put(inst, CanonicalName);
            }
        }

        // Store the list of instances for this context
        instanceMap.put(CanonicalName, contextInstances);

        if(debug){logger.fine("Assembler for " + CanonicalName
            + " subscribed to " + contextInstances.size() + " controls");}
    }

    @Override
    public void onControlChanged(ControlInstance instance, int newValue) {
        pendingNameUpdate = true;

        if (pendingTask != null)
            pendingTask.cancel(false);

        pendingTask = scheduler.schedule(() -> {
            if (pendingNameUpdate) {
                pendingNameUpdate = false;

                String assembled = assembleName(instance);
                String contextId = channelMap.get(instance);

                cacheName(contextId, assembled);

                listener.onChannelNameUpdated(contextId, assembled);
            }
        }, DEBOUNCE_MS, TimeUnit.MILLISECONDS);
    }

    private String assembleName(ControlInstance instance) {
        
        String context_key = channelMap.get(instance);
        if (context_key == null) {
            logger.warning("No context mapping for instance: " + instance.getCanonicalId());
            return "";
        }

        List<ControlInstance> NameInstances = instanceMap.get(context_key);
        if (NameInstances == null || NameInstances.isEmpty()) {
            logger.warning("No instance list for context: " + context_key);
            return "";
        }

        TreeMap<Integer, Character> chars = new TreeMap<>();

        for (ControlInstance ci : NameInstances){
            String subName = ci.getSubcontrol();

            String digits = subName.replaceAll("\\D+", "");
            if (digits.isEmpty()) continue;

            int pos = Integer.parseInt(digits);
            chars.put(pos, decodeAscii(ci.getValue()));
        }

        StringBuilder sb = new StringBuilder();
        for (char c : chars.values()) sb.append(c);

        int last = sb.length() - 1;
        while (last >= 0 && sb.charAt(last) == ' ') last--; //trim trailing whitespace

        if (last < 0) return "";
        return sb.substring(0, last + 1);
    }

    private char decodeAscii(int v) {
        if (v < 32 || v > 126)
            return ' ';
        return (char)v;
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
        if (deskLifeName != "")deskLifeName = "";
    }

    static void cacheName(String channelContext, String name){
        if (nameCache.containsKey(channelContext)){
            if (nameCache.get(channelContext) == name) return;
            nameCache.replace(channelContext,name);
            return;
        }
        nameCache.put(channelContext, name);
    }

    public static Map<String,String> getChannelNames(){
        return Map.copyOf(nameCache);
    }

    public void shutdownScheduler(){
        this.scheduler.shutdownNow();
    }
}
