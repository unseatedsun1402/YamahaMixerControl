package MidiControl.unit.Server;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import MidiControl.Server.MidiServer;
import MidiControl.TestUtilities.MidiTestUtils;

@Tag("unit")
public class MidiServerTest {
  private int getFirstAvailableDeviceIndex() {
        MidiDevice.Info[] devices = MidiSystem.getMidiDeviceInfo();
        return devices.length > 0 ? 0 : -1;
    }

    @Test
    void testSetOutputDeviceDoesNotThrow() {
        MidiServer server = new MidiServer();
        int index = getFirstAvailableDeviceIndex();

        if (index == -1) {
            System.out.println("No MIDI devices available; skipping testSetOutputDeviceDoesNotThrow");
            return;
        }

        assertDoesNotThrow(() -> server.getMidiDeviceManager().setOutputDevice(index));
    }

    @Test
    void testSetInputDeviceDoesNotThrow() {
        MidiServer server = new MidiServer();
        int index = getFirstAvailableDeviceIndex();

        if (index == -1) {
            System.out.println("No MIDI devices available; skipping testSetInputDeviceDoesNotThrow");
            return;
        }

        assertDoesNotThrow(() -> server.getMidiDeviceManager().setInputDevice(index));
    }

    @Test
    public void testClearBuffer(){
        MidiServer server = new MidiServer();
        MidiMessage testMessage;
        try {
            testMessage = MidiTestUtils.createSysexMessage(new byte[]{99,98,1});
            server.addtoinputqueue(testMessage);
            assertEquals(server.getInputBufferSize(), 1);
            server.clearInputBuffer();
            assertEquals(server.getInputBufferSize(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
}
