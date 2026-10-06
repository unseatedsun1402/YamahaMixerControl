package MidiControl.Server;

import MidiControl.ControlServer.HardwareInputHandler;
import MidiControl.Controls.ControlInstance;

public final class DebugController {

    private static boolean resolution;
    private static boolean canonical;

    private DebugController() {}

    public static void set(String flag, boolean enabled) {

        switch(flag) {

            case "resolution" -> {
                resolution = enabled;

                if(enabled)
                    ControlInstance.enableDebug();
                else
                    ControlInstance.disableDebug();
            }

            case "canonical" -> {
                canonical = enabled;

                if(enabled)
                    HardwareInputHandler.enableDebug();
                else
                    HardwareInputHandler.disableDebug();
            }
        }
    }

    public static boolean resolution() {
        return resolution;
    }

    public static boolean canonical() {
        return canonical;
    }
}
