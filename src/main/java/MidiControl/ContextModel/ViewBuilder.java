package MidiControl.ContextModel;

import java.util.List;

import MidiControl.Controls.CanonicalRegistry;

public interface ViewBuilder {
    List<ViewControl> build(Context context, CanonicalRegistry registry);
}
