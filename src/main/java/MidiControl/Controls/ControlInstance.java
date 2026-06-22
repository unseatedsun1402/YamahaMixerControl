package MidiControl.Controls;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import MidiControl.ControlServer.CanonicalInputEvent;
import MidiControl.NrpnUtils.NrpnMapping;
import MidiControl.NrpnUtils.NrpnMessage;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.UserInterface.ChannelName.ChannelNameAssembler;

public class ControlInstance {

    private final SubControl parent;
    private final int index;
    private static boolean debug = false;

    private final List<ControlListener> listeners = new ArrayList<>();

    private int value;
    private final String canonicalId;
    private NrpnMapping nrpnMapping;
    private SysexMapping sysexMapping;
    private static Logger logger = Logger.getLogger(ControlInstance.class.getName());
    private int priority = 3;
    private int canonMax = 1;
    private int canonMin = 0;

    public static void enableDebug(){
        debug = true;
    }

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
        if(sysex != null){
            this.priority= sysex.priority;
            this.canonMax=this.getMax();
            this.canonMin=this.getMin();
        }
    }

    public ControlInstance(String canonicalID, int index,SysexMapping sysex,NrpnMapping nrpn) {
        this.parent = null;
        this.index = index;
        this.sysexMapping = sysex;
        this.nrpnMapping = nrpn;
        this.canonicalId = canonicalID;
        if (sysexMapping !=null){
            this.priority= sysex.priority;
            this.canonMax=this.getMax();
            this.canonMin=this.getMin();
        }
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
        if(debug){logger.finer("Created a new listener" + listener.hashCode() + "for "+this.canonicalId);}
    }

    public int extractValue(CanonicalInputEvent event) {
        return switch (event.getType()) {
            case SYSEX -> sysexMapping.extractValue(event.getSysexData());
            case NRPN  -> {
                NrpnMessage eventnrpn = event.getNrpn();
                yield toCanonicalValue(eventnrpn.value);
            }
            case CC    -> event.getCc().getData2();
        };
    }

    private int toCanonicalValue(int rawValue) {
        NrpnMapping mapping = this.getNrpn().get();
        int min = 0;
        int max = mapping.getMax();
        if (max == min) return canonMin;

        double normalized = (double)(rawValue - min) / (max - min);
        double result = canonMin + normalized * (canonMax - canonMin);

        if(debug)logger.fine(String.format("%s scaling NRPN -> canonical: raw=%d -> %.2f",
            canonicalId, rawValue, result
        ));

        return (int)Math.round(result);
    }

    public int toNrpnValue(int canonicalValue, int min, int max) {
        if (canonMax == canonMin) return min;

        double normalized = (double)(canonicalValue - canonMin) / (double)(canonMax - canonMin);
        double result = min + normalized * (max - min);

        if(debug)logger.fine(String.format("%s scaling canonical -> NRPN: canonical %d -> %.2f",
            canonicalId, canonicalValue, result
        ));

        return (int)Math.round(result);
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

    public List<byte[]> buildSysexChange(int val) {
        byte[] msg = sysexMapping.buildChangeMessage(val, this.index);
        return List.of(msg);
    }

    public List<byte[]> buildNrpnChange(int val){
        if (getNrpn().isPresent()) {
            NrpnMapping mapping = getNrpn().get();
            return getNrpn().get().buildNrpnBytes(toNrpnValue(val, mapping.getMin(), mapping.getMax()));
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

    public void removeListener(ChannelNameAssembler channelNameAssembler) {
        this.listeners.remove(channelNameAssembler);
    }
}