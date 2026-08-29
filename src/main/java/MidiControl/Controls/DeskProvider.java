package MidiControl.Controls;

import MidiControl.SysexUtils.RegistryReloadListener;

public interface DeskProvider {
    public String getDeskType();
    public void addReloadListener(RegistryReloadListener l);
}
