package MidiControl.ControlMappings;

import MidiControl.NrpnUtils.NrpnMapping;
import MidiControl.SysexUtils.SysexMapping;

public class CanonicalMapping {
  private String canonical_key;
  private SysexMapping sysexMapping;
  private NrpnMapping nrpnMapping;
  private int value;
  private long lastUpdateTimestamp;
  private SOURCE lastUpdateSource;

  public static enum SOURCE {
    SYSEX,
    NRPN,
    UNKNOWN
  }

  public CanonicalMapping(String canonical_key, SysexMapping sysexMapping) {
    this.canonical_key = canonical_key;
    this.sysexMapping = sysexMapping;
  }

  public int updateValue(int value, SOURCE source) {
    this.lastUpdateTimestamp = System.currentTimeMillis();
    this.lastUpdateSource = source;
    return this.value = value;
  }

  public void setNrpnMapping(NrpnMapping nrpnMapping) {
    this.nrpnMapping = nrpnMapping;
  }

  public String getCanonicalId() {
    return this.canonical_key;
  }

  public SysexMapping getSysexMapping() {
    return sysexMapping;
  }

  public NrpnMapping getNrpnMapping() {
    return nrpnMapping;
  }

  public int getValue() {
    return value;
  }

  public long getLastUpdateTimestamp() {
    return lastUpdateTimestamp;
  }

  public SOURCE getLastUpdateSource() {
    return lastUpdateSource;
  }
}
