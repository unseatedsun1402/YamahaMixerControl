package MidiControl.ControlServer;

import javax.sound.midi.ShortMessage;

import MidiControl.NrpnUtils.NrpnMessage;

public class CanonicalInputEvent {

    public enum Type {
        SYSEX,
        NRPN,
        CC
    }

    private final Type type;

    // Only one of these will be non-null depending on type
    private final byte[] sysexData;
    private final NrpnMessage nrpn;
    private final ShortMessage cc;

    public CanonicalInputEvent(Type type, byte[] sysexData, NrpnMessage nrpn, ShortMessage cc) {
        this.type = type;
        this.sysexData = sysexData;
        this.nrpn = nrpn;
        this.cc = cc;
    }

    // Factory methods
    public static CanonicalInputEvent fromSysex(byte[] data) {
        return new CanonicalInputEvent(Type.SYSEX, data, null, null);
    }

    public static CanonicalInputEvent fromNrpn(NrpnMessage nrpn) {
        return new CanonicalInputEvent(Type.NRPN, null, nrpn, null);
    }

    public static CanonicalInputEvent fromCc(ShortMessage cc) {
        return new CanonicalInputEvent(Type.CC, null, null, cc);
    }

    // Getters
    public Type getType() {
        return type;
    }

    public byte[] getSysexData() {
        return sysexData;
    }

    public NrpnMessage getNrpn() {
        return nrpn;
    }

    public ShortMessage getCc() {
        return cc;
    }
}
