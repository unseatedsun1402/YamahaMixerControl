package MidiControl.Server;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.logging.Level; import java.util.logging.Logger;
import javax.sound.midi.MidiMessage;
import org.apache.commons.math3.exception.OutOfRangeException;
import MidiControl.ContextModel.BankCatalog;
import MidiControl.ContextModel.Context;
import MidiControl.ContextModel.ContextDiscoveryEngine;
import MidiControl.ContextModel.ViewRegistry;
import MidiControl.ContextModel.ControlSchema;
import MidiControl.ContextModel.InputChannelStripViewBuilder;
import MidiControl.ContextModel.MixAuxBusViewBuilder;
import MidiControl.ContextModel.ViewBuilder;
import MidiControl.ControlServer.HardwareInputHandler;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.SourceAllInstances;
import MidiControl.MidiDeviceManager.MidiIOManager;
import MidiControl.NrpnUtils.NrpnParser;
import MidiControl.NrpnUtils.NrpnRegistry;
import MidiControl.NrpnUtils.NrpnMapping;
import MidiControl.NrpnUtils.NrpnMappingLoader;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;
import MidiControl.UserInterface.UiBankFactory;
import MidiControl.UserInterface.UiContextIndex;
import MidiControl.UserInterface.UiModelFactory;
import MidiControl.UserInterface.UiModelService;
import MidiControl.UserInterface.DTO.UiModelDTO;
import MidiControl.UserInterface.Frontend.GuiBroadcastListener;
import MidiControl.UserInterface.Frontend.WebSocketGuiBroadcaster;
import jakarta.annotation.PreDestroy;
public class MidiServer implements Runnable, UiModelService{
    private volatile boolean shutdownFlag = false;
    private final ConcurrentLinkedQueue<MidiMessage> inputBuffer = new ConcurrentLinkedQueue<>();
    private final NrpnRegistry nrpnRegistry = new NrpnRegistry();
    private final NrpnParser nrpnParser = new NrpnParser();
    private final HardwareInputHandler inputHandler = new HardwareInputHandler(nrpnParser, nrpnRegistry);
    private final MidiIOManager deviceManager = new MidiIOManager(this);
    private final SubscriptionManager subscriptions;
    private final ControlSchema schema;
    private final ViewRegistry viewBuilders;
    private final UiContextIndex contextIndex;
    private final UiModelFactory uiFactory;
    private final UiBankFactory bankFactory;
    private final BankCatalog bankCatalog;
    private final ServerRouter serverRouter;
    private CanonicalRegistry canonicalRegistry;
    private GuiBroadcastListener guiBroadcastListener;
    private ContextDiscoveryEngine discoveryEngine;
    private static final GuiBroadcastListener NO_OP_LISTENER =
            new GuiBroadcastListener((json, ctx) -> {}, canonicalId -> null);
    private RehydrationManager rehydrationManager;

    private static final Logger logger = Logger.getLogger(MidiServer.class.getName());

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    // 1. Default constructor (used by ServletContextListener)
    public MidiServer() {

        List<SysexMapping> sysexMappings =
                // SysexMappingLoader.loadMappingsFromResource("MidiControl/m7cl_sysex_mappings.json");
                SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");
        this.canonicalRegistry =
                new CanonicalRegistry(sysexMappings, new SysexParser(sysexMappings));

        List<NrpnMapping> nrpnMappings =
                NrpnMappingLoader.loadFromResource("MidiControl/nrpn/01v96i_nrpn_mappings.json");
        // Attach NRPN mappings to ControlInstances
        try{
            this.canonicalRegistry.attachNrpnMappings(nrpnMappings);
        }
        catch (OutOfRangeException e) {
            logger.severe(e.toString());
        }
        this.viewBuilders = new ViewRegistry();
        this.uiFactory = null;
        // Safe fallback listener
        this.guiBroadcastListener = NO_OP_LISTENER;

        this.subscriptions = new SubscriptionManager();
        this.schema = new ControlSchema(canonicalRegistry);
        
        this.contextIndex = new UiContextIndex();

        this.viewBuilders.addView(new InputChannelStripViewBuilder(canonicalRegistry), "basic-input-view");
        this.viewBuilders.addView(new MixAuxBusViewBuilder(canonicalRegistry), "basic-master-view");
        
        this.guiBroadcastListener = new GuiBroadcastListener(new WebSocketGuiBroadcaster(subscriptions), contextIndex);
        this.bankCatalog = new BankCatalog();
        this.discoveryEngine = new ContextDiscoveryEngine(canonicalRegistry);
        this.bankFactory = new UiBankFactory(discoveryEngine, this);
        this.serverRouter = new ServerRouter(this,this.subscriptions,this.canonicalRegistry,this.deviceManager);
        this.rehydrationManager = new RehydrationManager(serverRouter.getOutputRouter(), (SourceAllInstances) this.canonicalRegistry, Executors.newSingleThreadScheduledExecutor());
        initContextIndex();
        serverRouter.injectApp(new App(rehydrationManager, deviceManager));
        logger.info("MidiServer: CanonicalRegistry initialized with SYSEX + NRPN mappings.");
    }

    // 2. Test constructor (registry only)
    public MidiServer(CanonicalRegistry registry) {
        this.canonicalRegistry = registry;
        this.viewBuilders = new ViewRegistry();
        this.viewBuilders.addView(new InputChannelStripViewBuilder(canonicalRegistry),"basic-input-view");
        this.uiFactory = null;
        this.bankFactory = null;
        this.bankCatalog = new BankCatalog();
        this.guiBroadcastListener = NO_OP_LISTENER;
        this.subscriptions = new SubscriptionManager();
        this.schema = new ControlSchema(canonicalRegistry);
        this.contextIndex = new UiContextIndex();
        this.guiBroadcastListener = new GuiBroadcastListener(new WebSocketGuiBroadcaster(subscriptions), contextIndex);
        this.serverRouter = new ServerRouter(this,this.subscriptions,this.canonicalRegistry,this.deviceManager);
        this.rehydrationManager = new RehydrationManager(serverRouter.getOutputRouter(),
            (SourceAllInstances) this.canonicalRegistry,
            Executors.newSingleThreadScheduledExecutor());

    }

    // 3. Test/production constructor (registry + real listener)
    public MidiServer(CanonicalRegistry registry, GuiBroadcastListener guiListener) {
        this.canonicalRegistry = registry;
        this.guiBroadcastListener = guiListener;
        this.subscriptions = new SubscriptionManager();
        this.schema = new ControlSchema(canonicalRegistry);
        this.viewBuilders = new ViewRegistry();
        this.viewBuilders.addView(new InputChannelStripViewBuilder(canonicalRegistry),"basic-input-view");
        this.contextIndex = new UiContextIndex();
        this.guiBroadcastListener = new GuiBroadcastListener(new WebSocketGuiBroadcaster(subscriptions), contextIndex);
        this.serverRouter = new ServerRouter(this,this.subscriptions,this.canonicalRegistry,this.deviceManager);
        this.uiFactory = null;
        this.bankFactory = null;
        this.bankCatalog = new BankCatalog();
        this.rehydrationManager = new RehydrationManager(serverRouter.getOutputRouter(),
            (SourceAllInstances) this.canonicalRegistry,
            Executors.newSingleThreadScheduledExecutor());
    }

    // Optional: setter for late injection
    public void setGuiBroadcastListener(GuiBroadcastListener listener) {
        this.guiBroadcastListener = listener;
    }


    public MidiIOManager getMidiDeviceManager() {
        return this.deviceManager;
    }

    public CanonicalRegistry getCanonicalRegistry() {
        return this.canonicalRegistry;
    }

    public void RehydrateSever(){
        this.rehydrationManager.rehydrateAll();
    }

    public void setCanonicalRegistry(CanonicalRegistry registry) {
        this.canonicalRegistry = registry;
    }

    public ConcurrentLinkedQueue<MidiMessage> getInputBuffer(){
        return this.inputBuffer;
    }

    public void addtoinputqueue(MidiMessage msg) {
        inputBuffer.add(msg);
    }

    public int getInputBufferSize() {
        return inputBuffer.size();
    }

    public ControlSchema getControlSchema() {
        return this.schema;
    }

    public Optional<ViewBuilder> getViewBuilder(String key) {
        return this.viewBuilders.getView(key);
    }

    public UiContextIndex getContextIndex() {
        return this.contextIndex;
    }

    public SubscriptionManager getSubscriptionManager() {
        return this.subscriptions;
    }

    public void clearInputBuffer() {
        inputBuffer.clear();
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Server is shutting down");
        deviceManager.shutdown();
        MidiProcessingLoop.shutdown();
        logger.info("Shutdown finished");
    }


    public void processIncomingMidiForTest() {
        new MidiProcessingLoop(inputBuffer, inputHandler, canonicalRegistry, guiBroadcastListener)
                .processIncomingMidi();
    }

    @Override
    public void run() {
        try {
            logger.info("MidiServer thread started.");

            Thread processingThread =
                new Thread(new MidiProcessingLoop(inputBuffer, inputHandler, canonicalRegistry, guiBroadcastListener),
                        "MidiProcessingThread");

            processingThread.setPriority(Thread.NORM_PRIORITY + 1);
            processingThread.setUncaughtExceptionHandler((t, e) ->
                logger.log(Level.SEVERE, "Processing thread crashed: " + t.getName(), e)
            );

            processingThread.start();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "MidiServer thread crashed", e);
        }
    }

    public ServerRouter getServerRouter() {
        return this.serverRouter;
    }

    public UiBankFactory getUiBankFactory() {
        return this.bankFactory;
    }

    public BankCatalog getBankCatalog() {
        return this.bankCatalog;
    }

    public UiModelFactory getUiModelFactory() {
        return this.uiFactory;
    }

    private void initContextIndex(){
        List<Context> contexts = discoveryEngine.discoverContexts();
        contextIndex.addAll(contexts);
    }

    @Override
    public UiModelDTO buildUiModel(String contextId, String uiType) {

        ViewBuilder builder = viewBuilders.getView(uiType)
                .orElseThrow(() -> new IllegalArgumentException("Unknown uiType: " + uiType));

        UiModelFactory factory = new UiModelFactory(
                canonicalRegistry,
                builder,
                contextIndex
        );

        return factory.buildUiModel(contextId);
    }
}
