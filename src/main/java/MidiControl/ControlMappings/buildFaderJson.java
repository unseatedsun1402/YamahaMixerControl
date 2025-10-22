package MidiControl.ControlMappings;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.gson.JsonObject;

import MidiControl.NrpnUtils.NrpnMapping;

public class buildFaderJson {
    public static String buildOutputFaderJson(NrpnMapping mapping, int value) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "outputFaderUpdate");
        json.addProperty("channelType", mapping.channelType().name());
        json.addProperty("channelIndex", mapping.channelIndex());
        json.addProperty("controlType", mapping.controlType().name());
        json.addProperty("value", value);
        json.addProperty("label", mapping.label());
        json.addProperty("msb", mapping.msb());
        json.addProperty("lsb", mapping.lsb());
        Logger.getLogger(buildFaderJson.class.getName()).log(Level.FINE, "Built output fader for msb %d lsb %d" + mapping.msb(),mapping.lsb());
        return json.toString();
    }

    public static String buildInputFaderJson(NrpnMapping mapping,int value){
        JsonObject json = new JsonObject();
        json.addProperty("type", "nrpnUpdate");
        json.addProperty("channelType", mapping.channelType().name());
        json.addProperty("channelIndex", mapping.channelIndex());
        json.addProperty("controlType", mapping.controlType().name());
        json.addProperty("value", value);
        json.addProperty("label", mapping.label());
        json.addProperty("msb", mapping.msb());
        json.addProperty("lsb", mapping.lsb());
        Logger.getLogger(buildFaderJson.class.getName()).log(Level.FINE, "Built input fader for msb %d lsb %d" + mapping.msb(),mapping.lsb());
        return json.toString();
    }
}
