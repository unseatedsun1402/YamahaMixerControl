package MidiControl.unit.SysexUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import MidiControl.SysexUtils.ModelNumbers;

public class ModelNumbersTest {
    @Test
    public void getModelByteTest(){
        byte expected = (byte)0x1A;
        String testKey = ModelNumbers.YAMAHA_01V96I.name();
        byte actual = ModelNumbers.getModelByteByString(testKey);
        assertEquals(expected, actual);
    }

        @Test
    public void getModelByteFailsTest(){
        String testKey = "noMap";
        byte actual = ModelNumbers.getModelByteByString(testKey);
        assertEquals(-1,actual);
    }
}
