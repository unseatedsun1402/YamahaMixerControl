package MidiControl.ControlServer;

import java.util.logging.Logger;

import MidiControl.Routing.OutputRouter;

public class GuiInputHandler {

    private final OutputRouter router;
    private final Logger logger = Logger.getLogger("GuiInputHandler");
    private static boolean debug = false;

    public static void enableDebug(){
        debug = true;
    }

    public GuiInputHandler(OutputRouter router) {
        this.router = router;
    }

    public void handleGuiChange(String canonicalId, int value) {
        router.applyChange(canonicalId, value);
        if(debug){logger.info("Handled Change from"+canonicalId+ " val: "+value);}
    }

    public void handleGuiRequest(String canonicalId) {
        router.applyRequest(canonicalId);
    }
}


