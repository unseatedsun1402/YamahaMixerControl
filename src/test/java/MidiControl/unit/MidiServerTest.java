package MidiControl.unit;

import static org.junit.jupiter.api.Assertions.fail;

import javax.sound.midi.MidiUnavailableException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import MidiControl.MidiServer;

@Tag("unit")
public class MidiServerTest {
        @Test
    
    void testSetOutputDeviceDoesNotThrow() {
        try {
            MidiServer.setOutputDevice(0); // Assumes device 0 is valid in test environment
        } catch (MidiUnavailableException e) {
            fail("Device setup should not throw: " + e.getMessage());
        }
    }

    @Test
    void testIOConfigureDoesNotThrow() {
        try {
            MidiServer.configureIO(1,0); // Assumes device 0 is valid in test environment
        } catch (MidiUnavailableException e) {
            fail("Device setup should not throw: " + e.getMessage());
        }
    }

    @Test
    void testSetInputDeviceDoesNotThrow() {
        try {
            MidiServer.setInputDevice(0); // Assumes device 0 is valid in test environment
        } catch (MidiUnavailableException e) {
            fail("Device setup should not throw: " + e.getMessage());
        }
    }
}
