package MidiControl.functional;

import java.io.File;
import java.util.logging.Logger;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.SysexMessage;

import org.junit.jupiter.api.Test;

import MidiControl.MidiServer;
import MidiControl.SysexUtils.*;
import MidiControl.TestUtilities.MidiTestUtils;
import MidiControl.NrpnUtils.*;

public class SendSysexChangeTest {
    @org.junit.jupiter.api.Test
    void testSysExReception() throws Exception {
        String mappingPath = "MidiControl/nrpn_mappings.json";
        NrpnParser nrpnParser = new NrpnParser();
        NrpnRegistry.INSTANCE.loadFromJson(mappingPath);
        Logger.getLogger(MidiServer.class.getName()).info("Loaded NRPN mappings from external file.");

        MidiControl.ControlServer.InputHandler inputHandler = new MidiControl.ControlServer.InputHandler(nrpnParser);
        SysexRegistry sysexRegistry = new SysexRegistry(SysexMappingLoader.loadAllMappings());
        
        NrpnRegistry.getInstance().buildProfileMapping(); // Ensure mappings are loaded

        MidiServer server = new MidiServer();
        
        byte[] faderMsg = new byte[] {
            (byte)0xF0, (byte)0x43, (byte)0x10, (byte)0x3E,
            (byte)0x7F, (byte)0x01, (byte)0x1C, (byte)0x00,
            (byte)0x1A, (byte)0x00, (byte)0x00, (byte)0x03,
            (byte)0x14, (byte)0xF7
        };
        
        MidiServer.addtoinputqueue(MidiTestUtils.createSysexMessage(faderMsg));
        server.processIncomingMidi();

        // Mock registry to return the expected control
        // MidiControl.Controls.Control expected =
        //         new MidiControl.Controls.Control("kInputHA", 41, "kHAPhantom");

        // when(controlRegistry.resolveSysex(sysex)).thenReturn(expected);

        // // Push message into server buffer
        // server.inputBuffer.add(new SysexMessage(sysex, sysex.length));

        // // Act
        // server.processIncomingMidi();

        // // Assert
        // verify(controlRegistry).resolveSysex(sysex);
    }
}