package MidiControl.MidiDeviceManager;

public enum TransportMode {
    SYSEX,   // Default: Yamaha-native, full fidelity
    NRPN,    // Optional: only works if NRPN mapping exists
    CC       // Optional: only works if CC mapping exists
}
