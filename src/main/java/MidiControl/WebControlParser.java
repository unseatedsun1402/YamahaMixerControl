package MidiControl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.sound.midi.ShortMessage;

/**
 * Parses JSON from GUI and converts to NRPN MIDI messages.
 *
 * @author ethanblood
 */
public class WebControlParser {

  public static ShortMessage[] parseJSON(String message) {
    ShortMessage[] command = new ShortMessage[4];

    try {
      JsonObject jsonobject = JsonParser.parseString(message).getAsJsonObject();

      String idStr = jsonobject.get("id").getAsString(); // e.g. "ch107"
      int value = jsonobject.get("value").getAsInt(); // 0–127

      // NEW: Use msb and lsb directly from JSON
      int msb = jsonobject.has("msb") ? jsonobject.get("msb").getAsInt() : 0;
      int lsb = jsonobject.has("lsb") ? jsonobject.get("lsb").getAsInt() : 0;

      System.out.printf(
          "Parsed JSON -> ID=%s, Value=%d -> NRPN (MSB=%d, LSB=%d)%n", idStr, value, msb, lsb);

      // NRPN LSB (CC 98)
      command[0] = new ShortMessage();
      command[0].setMessage(ShortMessage.CONTROL_CHANGE, 0, 98, lsb);

      // NRPN MSB (CC 99)
      command[1] = new ShortMessage();
      command[1].setMessage(ShortMessage.CONTROL_CHANGE, 0, 99, msb);

      // Data Entry MSB (CC 6)
      command[2] = new ShortMessage();
      command[2].setMessage(ShortMessage.CONTROL_CHANGE, 0, 6, value);

      // Data Entry LSB (CC 38) — optional, set to 0 for 7-bit resolution
      command[3] = new ShortMessage();
      command[3].setMessage(ShortMessage.CONTROL_CHANGE, 0, 38, 0);

      // Debug output
      for (int i = 0; i < command.length; i++) {
        System.out.printf(
            "MIDI[%d] -> Cmd=%d, Ch=0, CC=%d, Val=%d%n",
            i, command[i].getCommand(), command[i].getData1(), command[i].getData2());
      }

    } catch (Exception e) {
      System.err.println("Error parsing MIDI JSON: " + e);
    }

    return command;
  }
}
