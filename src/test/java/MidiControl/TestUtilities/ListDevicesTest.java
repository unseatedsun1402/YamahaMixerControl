package MidiControl.TestUtilities;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import javax.sound.midi.MidiUnavailableException;

public class ListDevicesTest {
    @org.junit.jupiter.api.Test
    void testListDevices() {
        assertDoesNotThrow(() -> MidiControl.ListDevices.listDevices(), "Listing MIDI devices should not throw an exception");
    }
}
