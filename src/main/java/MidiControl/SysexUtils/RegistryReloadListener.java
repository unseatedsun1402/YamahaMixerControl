package MidiControl.SysexUtils;

import MidiControl.Controls.CanonicalRegistry;

public interface RegistryReloadListener {
    void onRegistryReloaded(CanonicalRegistry newRegistry);
}
