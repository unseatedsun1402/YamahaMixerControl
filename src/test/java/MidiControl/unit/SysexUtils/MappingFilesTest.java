package MidiControl.unit.SysexUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import MidiControl.SysexUtils.MappingFiles;

public class MappingFilesTest {
    @Test
    public void get01VFile(){
        String testString="MidiControl/01v96i_sysex_mappings.json";
        String nameString="Yamaha_01V96I";
        System.out.println("Name "+MappingFiles.getFilePathByKey(nameString));
        assertEquals(testString, MappingFiles.getFilePathByKey(nameString));
    }

    @Test
    public void getM7CLFile(){
        String testString="MidiControl/m7cl_sysex_mappings.json";
        String nameString="Yamaha_M7CL";
        System.out.println("Name "+MappingFiles.getFilePathByKey(nameString));
        assertEquals(testString, MappingFiles.getFilePathByKey(nameString));
    }

    @Test
    public void getInvalidModelReturnsNull(){
        String nameString="Yamaha_M8NA";
        System.out.println("Name "+MappingFiles.getFilePathByKey(nameString));
        assertNull(MappingFiles.getFilePathByKey(nameString), "Returned val was not null");
    }
}
