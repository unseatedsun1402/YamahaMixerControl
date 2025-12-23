package MidiControl.UserInterface;

import MidiControl.ContextModel.Context;
import java.util.Arrays;

public class LayoutPresets {

    public static Layout forContext(Context ctx) {
        Layout layout = new Layout();

        if (ctx.getId().startsWith("channel")) {
            layout.type = "channel-strip";
            layout.sections = Arrays.asList("name", "fader", "pan", "eq", "sends");
        } else if (ctx.getId().startsWith("mix")) {
            layout.type = "mix-strip";
            layout.sections = Arrays.asList("name", "fader", "sends");
        } else {
            layout.type = "generic";
            layout.sections = Arrays.asList("controls");
        }

        return layout;
    }
}
