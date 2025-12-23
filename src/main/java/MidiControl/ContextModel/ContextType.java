package MidiControl.ContextModel;

public enum ContextType {
    MIX,        // Aux mixes, stereo bus, matrix bus
    CHANNEL,    // Individual channel views (fader, EQ, dynamics, sends)
    SYSTEM      // System settings, routing, user management
, BUS, MATRIX, DCA, STEREO_INPUT
}