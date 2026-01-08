package MidiControl.unit.MidiDeviceManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import MidiControl.MidiDeviceManager.MidiOutput;
import MidiControl.Mocks.MockMidiOut;
import MidiControl.MidiDeviceManager.MidiIOManager;

public class MidiIoManagerTest {

    @Test
    public void testShutdown(){
        MidiIOManager io = new MidiIOManager();
        MidiOutput out = new MockMidiOut();
        io.setMidiOutForTest(out);
        assertTrue(io.getMidiOut().isOpen());
        assertNotNull(io.getMidiOut());
        assertDoesNotThrow(() -> io.shutdown());
        assertTrue(out.isOpen() == false);
    }
        
}
