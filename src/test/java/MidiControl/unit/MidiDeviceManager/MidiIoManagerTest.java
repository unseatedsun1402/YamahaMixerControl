package MidiControl.unit.MidiDeviceManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import MidiControl.MidiDeviceManager.MidiOutput;
import MidiControl.Mocks.MockMidiOut;
import MidiControl.MidiDeviceManager.MidiIOManager;
import MidiControl.MidiDeviceManager.MidiDeviceUtils;
import MidiControl.MidiDeviceManager.InputWrapper;
import MidiControl.MidiDeviceManager.ReceiverWrapper;
import MidiControl.MidiDeviceManager.MidiSendEngine.ThroughputProfile;
import MidiControl.MidiDeviceManager.Simulator.M7CLSimulatorMidiDevice;
import java.util.concurrent.ConcurrentLinkedQueue;

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

    @Test
    void throughputCanBeConfiguredWithoutAnOutput() {
        MidiIOManager io = new MidiIOManager();
        io.setThroughputProfile(ThroughputProfile.FAST_USB);
        assertEquals(ThroughputProfile.FAST_USB, io.getThroughputProfile());
        io.shutdown();
    }

    @Test
    void failedOutputDoesNotLeaveAStalePortForRecovery() {
        MidiIOManager io = new MidiIOManager();
        int simulator = MidiDeviceUtils.getDeviceIndex(M7CLSimulatorMidiDevice.getSimulatorInfo());
        try {
            assertTrue(io.trySetOutputDevice(simulator));
            assertFalse(io.trySetOutputDevice(-1));
            assertNull(io.getMidiOut());
            assertFalse(io.resetMidiSubsystem());
            assertNull(io.getMidiOut(), "Recovery must not reopen an unrelated output");
        } finally {
            io.shutdown();
        }

    }

    @Test
    void replacingOneSideOfSharedDeviceDoesNotCloseTheOther() throws Exception {
        MidiIOManager io = new MidiIOManager();
        M7CLSimulatorMidiDevice device = new M7CLSimulatorMidiDevice();
        InputWrapper input = new InputWrapper(device, new ConcurrentLinkedQueue<>());
        var inputField = MidiIOManager.class.getDeclaredField("midiIn");
        inputField.setAccessible(true);
        inputField.set(io, input);
        io.setMidiOutForTest(new ReceiverWrapper(device));
        try {
            assertFalse(io.trySetOutputDevice(-1));
            assertTrue(input.isOpen(), "Closing output must preserve the shared input");

            io.setMidiOutForTest(new ReceiverWrapper(device));
            assertFalse(io.trySetInputDevice(-1));
            assertTrue(io.getMidiOut().isOpen(), "Closing input must preserve the shared output");
        } finally {
            io.shutdown();
        }
        assertFalse(device.isOpen());
    }
        
}
