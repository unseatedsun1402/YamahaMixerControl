package MidiControl.SysexUtils;

import java.util.List;
import java.util.logging.Logger;

public class SysexParser {
  private static final Logger logger = Logger.getLogger(SysexParser.class.getName());
  private final SysexRegistry registry;

    public SysexParser(List<SysexMapping> mappings) {
        this.registry = new SysexRegistry(mappings);
    }

  public SysexMapping processMidiMessage(byte[] message) {
    SysexMapping mapping = registry.resolve(message);
    if (mapping != null) {
      logger.info(mapping.getControlGroup() +" "+ mapping.getSubControl());
      return mapping;
    }
    logger.warning("Unrecognized Sysex message: " + bytesToHex(message));
    return null;
  }

  public static String bytesToHex(byte[] message) {
    StringBuilder sb = new StringBuilder();
    for (byte b : message) {
      sb.append(String.format("%02X", b));
    }
    return sb.toString();
  }
}
