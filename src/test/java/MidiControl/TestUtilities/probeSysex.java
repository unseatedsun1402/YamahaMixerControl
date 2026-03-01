package MidiControl.TestUtilities;

import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;

import java.util.List;

import org.junit.jupiter.api.Test;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.MidiDeviceManager.*;

public class probeSysex {
    private static MidiIOManager manager = new MidiIOManager();
    private static int testInputDevice = 0;
    private static int testOutputDevice = 1;

    static{
        if(!manager.trySetInputDevice(testInputDevice))System.out.println("Error, input device set failed "+testInputDevice);
        if(!manager.trySetOutputDevice(testOutputDevice))System.out.println("Error, output device set failed "+testOutputDevice);
    }

    public byte[] buildSysex(SysexMapping sysexMapping,int index){
        return         sysexMapping.buildRequestMessage(index);
    }

    public void sendSysex(byte[] message){
        manager.sendAsync(message);
    }

    @Test
    public void getMidiResponseOf(){
        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromResource("MidiControl/m7cl_sysex_mappings.json");
        CanonicalRegistry registry = new CanonicalRegistry(mappings,new SysexParser(mappings));
        SysexMapping toCheck = registry.getGroup("kNameInputChannel")
            .getSubcontrol("kNameShort1")
                .getInstances()
                    .get(0).getSysex();
        byte[] message = buildSysex(toCheck, 0);
        System.out.println("Test message: "+SysexParser.bytesToHex(message));
        sendSysex(message);
        }
}
