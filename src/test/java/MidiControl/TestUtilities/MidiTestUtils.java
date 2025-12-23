package MidiControl.TestUtilities;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;

public class MidiTestUtils {
  public static ShortMessage createControlChange(int channel, int data1, int data2) {
    try {
      return new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, data1, data2);
    } catch (InvalidMidiDataException e) {
      throw new RuntimeException("Failed to create ShortMessage", e);
    }
  }

  public static void assertJsonContains(String json, String key, String expectedValue) {
    assertTrue(
        json.contains("\"" + key + "\":" + expectedValue)
            || json.contains("\"" + key + "\":\"" + expectedValue + "\""),
        "JSON should contain " + key + " with value " + expectedValue);
  }

  public static boolean isValidJson(String jsonString) {
    try {
      JsonParser.parseString(jsonString);
      return true;
    } catch (JsonSyntaxException e) {
      return false;
    }
  }

  public static SysexMessage createSysexMessage(byte[] data) throws Exception {
    try {
      SysexMessage msg = new SysexMessage();
      msg.setMessage(data, data.length);
      return msg;
    }
    catch (Exception e) {
      System.err.println(e);
    }
    return null;
  }
}
