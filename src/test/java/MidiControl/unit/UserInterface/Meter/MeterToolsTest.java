package MidiControl.unit.UserInterface.Meter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import MidiControl.UserInterface.Meter.MeterTools;

public class MeterToolsTest {
    @Test
    public void testLongToStringdB(){
        int TestValue = 9050;
        String expected = "90.50 dB";
        String actual = MeterTools.formatCentiDb(TestValue);
        assertEquals(expected, actual);
    }

    @Test
    void testFormatInfinity() {
        assertEquals("-inf dB", MeterTools.formatCentiDb(Long.MIN_VALUE));
    }

    @Test
    public void testBytesToCenti(){
        byte[] TestValue = {0x24};
        int bytesPer = 1;
        long expected = -6400;
        long actual = MeterTools.toCentiDb(TestValue,bytesPer);
        assertEquals(expected, actual);
    }

    @Test
    public void test2BytesToCenti(){
        byte[] TestValue = {0xA,(byte) 0x02};
        int bytesPer = 2;
        long expected = -6300;
        long actual = MeterTools.toCentiDb(TestValue,bytesPer);
        assertEquals(expected, actual);
    }
    
    @Test
    void testFormatNegativeCentiDb() {
        assertEquals("-12.34 dB", MeterTools.formatCentiDb(-1234));
    }

    @Test
    void testFormatZeroCentiDb() {
        assertEquals("0.00 dB", MeterTools.formatCentiDb(0));
    }

    @Test void testMixerLookupValueWidth(){
        int expected,actual;

        int Y01VByte = 0x1E;
        expected = 1;
        actual = MeterTools.bytesPerForModel(Y01VByte);
        assertEquals(expected, actual);
        
        int YM7CByte = 0x1A;
        expected = 2;
        actual = MeterTools.bytesPerForModel(YM7CByte);
        assertEquals(expected, actual);
    }

    @Test
    void testNullRawReturnsMinValue() {
        long v = MeterTools.toCentiDb(null, 1);
        assertEquals(Long.MIN_VALUE, v);
    }

    @Test
    void testInvalidBytesPerReturnsMinValue() {
        byte[] raw = { 0x20 };
        assertEquals(Long.MIN_VALUE, MeterTools.toCentiDb(raw, 0));
        assertEquals(Long.MIN_VALUE, MeterTools.toCentiDb(raw, 3));
    }

    @Test
    void testNativeDisabled(){
        assertDoesNotThrow(() -> MeterTools.disableNativeForTests());
    }

    @Test
    void testFallbackJavaDoesNotFailOnBadSysex(){
        assertDoesNotThrow(() -> MeterTools.disableNativeForTests());
        byte[] rawBytes =  {(byte) 0xF0,0x43,0x10,0x11,0x01,(byte) 0xF7};
        var result = MeterTools.convertSingle(rawBytes, 1);
        assertEquals(result, 0);
    }

}
