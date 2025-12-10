package MidiControl.functional;

import MidiControl.SysexUtils.*;

public class SendSysexChangeTest {
    public static void main(String[] args) throws Exception {
        // SysexParser parser = new SysexParser();
        // MidiControl.TestUtilities.SysexMocker mocker = new MidiControl.TestUtilities.SysexMocker(parser);

        // // Optional: set a dispatch target
        // SysexParser.setDispatchTarget(new SysexDispatchTarget() {
        //     public void handleResolvedSysex(SysexMapping mapping, byte[] data) {
        //         System.out.println("Dispatched: " + mapping.label() + " → " + bytesToHex(data));
        //     }

        //     private String bytesToHex(byte[] bytes) {
        //         StringBuilder sb = new StringBuilder();
        //         for (byte b : bytes) {
        //             sb.append(String.format("%02X ", b));
        //         }
        //         return sb.toString().trim();
        //     }
        // });

        // // Load registry
        // // SysexRegistry.getInstance().loadFromJson("sysex_mappings.json");

        // // Send a known Sysex message
        // mocker.sendSysex(new byte[] {(byte)0xF0, 0x43, 0x12, 0x00, 0x7F, 0xF7}); // Example Sysex message
    }
}