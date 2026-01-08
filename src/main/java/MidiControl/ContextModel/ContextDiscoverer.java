package MidiControl.ContextModel;

import java.util.List;

import MidiControl.Controls.CanonicalRegistry;

public interface ContextDiscoverer {
    void discover(List<Context> out, CanonicalRegistry registry);
}

