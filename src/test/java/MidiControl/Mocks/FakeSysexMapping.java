package MidiControl.Mocks;

import java.util.List;

import MidiControl.SysexUtils.SysexMapping;

public class FakeSysexMapping {
    public static List<SysexMapping> fakeSysexMapping(){
        String json =
            "[{"
                + "\"control_group\": \"kInputHA\","
                + "\"control_id\": 41,"
                + "\"max_channels\": 56,"
                + "\"sub_control\": \"kHAPhantom\","
                + "\"value\": 0,"
                + "\"min_value\": 0,"
                + "\"max_value\": 1,"
                + "\"default_value\": 0,"
                + "\"comment\": \"Off, On\","
                + "\"key\": 18695995326464,"
                + "\"address_bytes\": [4,5,6,7,8,9],"
                + "\"index_bytes\": [10,11],"
                + "\"parameter_change_format\": ["
                + "240,67,\"1n\",62,17,1,0,41,0,0,"
                + "\"cc\",\"cc\","
                + "\"dd\",\"dd\",\"dd\",\"dd\",\"dd\","
                + "247"
                + "],"
                + "\"parameter_request_format\": ["
                + "240,67,\"3n\",62,17,1,0,41,0,0,"
                + "\"cc\",\"cc\",247"
                + "]"
            + "}]";
        return MidiControl.SysexUtils.SysexMappingLoader.loadMappingsFromString(json);
    }

    public static byte[] testRequest(){
        byte[] msg = new byte[] {
            (byte)0xF0, (byte)0x43, (byte)0x10, (byte)0x3E,
            (byte)0x11, (byte)0x01, (byte)0x00,
            (byte)0x29, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x29,   // correct index bytes
            (byte)0x01, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00,
            (byte)0xF7
        };
        return msg;
    }
}
