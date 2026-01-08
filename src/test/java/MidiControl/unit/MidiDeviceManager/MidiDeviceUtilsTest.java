package MidiControl.unit.MidiDeviceManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;

import org.junit.jupiter.api.Test;

public class MidiDeviceUtilsTest {
    private int getFirstAvailableDeviceIndex() {
        MidiDevice.Info[] devices = MidiSystem.getMidiDeviceInfo();
        return devices.length > 0 ? 0 : -1;
    }

    @Test
    public void testGetInfo(){
        int testDevice = getFirstAvailableDeviceIndex();
        assertTrue(testDevice >= 0);
        MidiDevice.Info info;
        try {
            info = MidiControl.MidiDeviceManager.MidiDeviceUtils.getInfo(testDevice);
            assertNotNull(info);
        } catch (MidiUnavailableException e) {
            System.out.println("Failed to capture the info of device "+testDevice);
        }
    }

    @Test
    public void testGetInfoForBadIndex(){
        MidiDevice.Info info;
        int badIndex = -999;
        try {
            info = MidiControl.MidiDeviceManager.MidiDeviceUtils.getInfo(badIndex);
            assertEquals(-1,info);
        } catch (MidiUnavailableException e) {
            System.out.println("Failed to capture the info of device "+ badIndex);
        }
    }

    @Test
    public void testGetDeviceFromInfo(){
        int testDevice = getFirstAvailableDeviceIndex();
        MidiDevice.Info info;
        try {
            info = MidiControl.MidiDeviceManager.MidiDeviceUtils.getInfo(testDevice);
            MidiControl.MidiDeviceManager.MidiDeviceUtils.getDeviceIndex(info);
        } catch (MidiUnavailableException e) {
            System.out.println("Failed to capture the info of device "+testDevice);
        }
    }

        @Test
    public void testGetDeviceFromBadInfo(){
        MidiDevice.Info info = null;
        assertEquals(MidiControl.MidiDeviceManager.MidiDeviceUtils.getDeviceIndex(info),-1);
    }
        
}
