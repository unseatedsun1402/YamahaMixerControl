package MidiControl.Controls;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import MidiControl.ControlServer.CanonicalInputEvent;
import MidiControl.NrpnUtils.NrpnMapping;
import MidiControl.NrpnUtils.NrpnMessage;
import MidiControl.SysexUtils.SysexMapping;

public class ControlInstance {

    private final SubControl parent;
    private final int index;

    private final List<ControlListener> listeners = new ArrayList<>();

    private int value;
    private final String canonicalId;
    private NrpnMapping nrpnMapping;
    private SysexMapping sysexMapping;
    private static Logger logger = Logger.getLogger(ControlInstance.class.getName());
    private int priority = 3;

    public ControlInstance(SubControl parent,
                        int index,
                        SysexMapping sysex,
                        NrpnMapping nrpn) {
        this.parent = parent;
        this.index = index;
        this.sysexMapping = sysex;
        this.nrpnMapping = nrpn;
        this.canonicalId =
            parent.getParentGroup().getName() + "." +
            parent.getName() + "." +
            index;
        if(sysex != null){this.priority= sysex.priority;}
    }
    
    public byte getResolution() {
            int range = getMax() - getMin();

            if (range <= 127) return (byte)0x0F;        // 7-bit
            if (range <= 1023) return (byte) 0xF0;      // 10-bit
            return (byte)0xFF;                          // 14-bit
        }
    
    public int getPriority(){
        return this.priority;
    }

    public void setPriority(int priority){
        this.priority = priority;
    }

    public void addListener(ControlListener listener) {
        listeners.add(listener);
        logger.fine("Created a new listener" + listener.hashCode() + "for "+this.canonicalId);
    }

    public int extractValue(CanonicalInputEvent event) {
        return switch (event.getType()) {
            case SYSEX -> sysexMapping.extractValue(event.getSysexData());
            case NRPN  -> {
                NrpnMessage eventnrpn = event.getNrpn();
                yield eventnrpn.value;
            }
            case CC    -> event.getCc().getData2();
        };
    }

    public int getIndex() {
        return index;
    }

    public int getValue() {
        return this.value;
    }

    public int updateValue(int value) {
        this.value = value;

        for (ControlListener l : listeners) {
            l.onControlChanged(this, value);
        }
        logger.fine("New value for "+this.canonicalId + " is: "+ this.value);
        return this.value;
    }

    public SysexMapping getSysex() {
        return sysexMapping;
    }

    public Optional<NrpnMapping> getNrpn() {
        return Optional.ofNullable(nrpnMapping);
    }

    public void setNrpn(NrpnMapping nrpn) {
        this.nrpnMapping = nrpn;
    }

    public String getCanonicalId() {
        return this.canonicalId;
    }

    public SubControl getParent() {
        return parent;
    }

    public List<byte[]> buildMidiRequest() {
        byte[] msg = sysexMapping.buildRequestMessage(this.index);
        return List.of(msg);
    }

    public List<byte[]> buildSysexChange(int build) {
        byte[] msg = sysexMapping.buildChangeMessage(build, this.index);
        return List.of(msg);
    }

    public List<byte[]> buildNrpnChange(int build){
        if (getNrpn().isPresent()) {
            return getNrpn().get().buildNrpnBytes(Optional.of(this),build);
        }
        return null;
    }

    public boolean hasChangeMapping() {
        if (this.sysexMapping.getParameter_change_format() != null) { return true; }
        return false;
    }

    public boolean hasRequestMapping() {
        if (this.sysexMapping.getParameter_request_format() != null) { return true; }
        return false;
    }

    public void setValue(int val) {
        this.value = val;
    }

    public String getGroup() {
        return parent.getParentGroup().getName();
    }

    public String getSubcontrol() {
        return parent.getName();
    }

    public int getInstanceIndex() {
        return this.index;
    }

    public int getMin() {
        return sysexMapping.getMin_value();
    }

    public int getMax() {
        return sysexMapping.getMax_value();
    }
}