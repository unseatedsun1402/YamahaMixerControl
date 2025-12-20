package MidiControl.TestUtilities;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import com.google.gson.JsonSyntaxException;

import MidiControl.NrpnUtils.NrpnMapping;
import MidiControl.NrpnUtils.NrpnParser;

import com.google.gson.JsonParser;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MidiTestUtils {
    public static ShortMessage createControlChange(int channel, int data1, int data2) {
        try {
            return new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, data1, data2);
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException("Failed to create ShortMessage", e);
        }
    }

    public static void sendMockNrpn(NrpnParser parser, int msb, int lsb, int value) {
        int valueMsb = (value >> 7) & 0x7F;
        int valueLsb = value & 0x7F;

        parser.parse(createControlChange(0, 99, msb));
        parser.parse(createControlChange(0, 98, lsb));
        parser.parse(createControlChange(0, 6, valueMsb));
        parser.parse(createControlChange(0, 38, valueLsb));
    }

    public static void assertJsonContains(String json, String key, String expectedValue) {
        assertTrue(json.contains("\"" + key + "\":" + expectedValue) || json.contains("\"" + key + "\":\"" + expectedValue + "\""),
            "JSON should contain " + key + " with value " + expectedValue);
    }

    public static String buildNrpnJson(NrpnMapping mapping, int value) {
    return "{"
        + "\"type\":\"nrpnUpdate\","
        + "\"channelType\":\"" + mapping.channelType() + "\","
        + "\"channelIndex\":" + mapping.channelIndex() + ","
        + "\"controlType\":\"" + mapping.controlType() + "\","
        + "\"value\":" + value + ","
        + "\"msb\":" + mapping.msb() + ","
        + "\"lsb\":" + mapping.lsb()
        + "}";
}

    public static boolean isValidJson(String jsonString) {
        try {
            JsonParser.parseString(jsonString);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }

    public static MidiMessage createSysexMessage(byte[] message) throws InvalidMidiDataException {
        MidiMessage sysex = new javax.sound.midi.SysexMessage(message, message.length);
        return sysex;
    }
}
