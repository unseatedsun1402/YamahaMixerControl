package MidiControl;

import MidiControl.ChannelMappings.ControlType;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.sound.midi.ShortMessage;

public class ParseMidi {
  /**
   * Parses a ShortMessage into its components
   *
   * @param message
   * @return int[]
   */
  public static String resolveControlType(ShortMessage message) {
    int data1 = message.getData1();
    for (ControlType type : ControlType.values()) {
      if (type.getControlByte() == data1) {
        return type.name();
      }
    }
    return null;
  }

  public static void printResolvedTypes(ShortMessage message) {
    String controlType = resolveControlType(message);
    if (controlType != null) {
      System.out.println("Control Type: " + controlType + ", Value: " + message.getData2());
    } else {
      System.out.println("Unknown Control Type for data1: " + message.getData1());
    }
  }

  public static ShortMessage[] parseRawMidiJson(String json) {
    try {
      JsonArray array = JsonParser.parseString(json).getAsJsonArray();
      ShortMessage[] messages = new ShortMessage[array.size()];

      for (int i = 0; i < array.size(); i++) {
        JsonObject obj = array.get(i).getAsJsonObject();

        int command = obj.get("command").getAsInt();
        int data1 = obj.get("data1").getAsInt();
        int data2 = obj.get("data2").getAsInt();
        int channel = obj.get("channel").getAsInt();

        ShortMessage msg = new ShortMessage();
        msg.setMessage(command, channel, data1, data2);
        messages[i] = msg;
      }

      return messages;
    } catch (Exception e) {
      System.err.println("Error parsing raw MIDI JSON: " + e);
      return new ShortMessage[0];
    }
  }
}
