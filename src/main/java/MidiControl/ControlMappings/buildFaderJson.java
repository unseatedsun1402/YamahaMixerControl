package MidiControl.ControlMappings;

import MidiControl.ChannelMappings.ChannelType;
import MidiControl.NrpnUtils.NrpnMapping;
import com.google.gson.JsonObject;
import java.util.logging.Level;
import java.util.logging.Logger;

public class buildFaderJson {

  public static String buildOutputFaderJson(NrpnMapping mapping, int value) {
    JsonObject json = new JsonObject();
    ChannelType type = mapping.channelType();
    String typeName = (type != null) ? type.name() : "UNKNOWN";
    json.addProperty("type", "outputFaderUpdate");
    json.addProperty("channelType", typeName);
    json.addProperty("channelIndex", mapping.channelIndex());
    json.addProperty("controlType", mapping.controlType().name());
    json.addProperty("value", value);
    json.addProperty("label", mapping.label());
    json.addProperty("msb", mapping.msb());
    json.addProperty("lsb", mapping.lsb());

    Logger.getLogger(buildFaderJson.class.getName())
        .log(
            Level.FINE,
            String.format("Built output fader for msb %d lsb %d", mapping.msb(), mapping.lsb()));

    return json.toString();
  }

  public static String buildInputFaderJson(NrpnMapping mapping, int value) {
    JsonObject json = new JsonObject();
    ChannelType type = mapping.channelType();
    String typeName = (type != null) ? type.name() : "UNKNOWN";
    json.addProperty("type", "nrpnUpdate");
    json.addProperty("channelType", typeName);
    json.addProperty("channelIndex", mapping.channelIndex());
    json.addProperty("controlType", mapping.controlType().name());
    json.addProperty("value", value);
    json.addProperty("label", mapping.label());
    json.addProperty("msb", mapping.msb());
    json.addProperty("lsb", mapping.lsb());

    Logger.getLogger(buildFaderJson.class.getName())
        .log(
            Level.FINE,
            String.format("Built input fader for msb %d lsb %d", mapping.msb(), mapping.lsb()));

    return json.toString();
  }
}
