package MidiControl.unit.Server;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import MidiControl.Server.MidiServer;
import MidiControl.TestUtilities.MidiTestUtils;

@Tag("unit")
public class MidiServerTest {
  private final List<MidiServer> servers = new ArrayList<>();

  private MidiServer createServer() {
    MidiServer server = new MidiServer();
    servers.add(server);
    return server;
  }

  @AfterEach
  void shutdownServers() {
    servers.forEach(MidiServer::shutdown);
    servers.clear();
  }

  private int getFirstAvailableDeviceIndex() {
        MidiDevice.Info[] devices = MidiSystem.getMidiDeviceInfo();
        return devices.length > 0 ? 0 : -1;
    }

    @Test
    void testSetOutputDeviceDoesNotThrow() {
        MidiServer server = createServer();
        int index = getFirstAvailableDeviceIndex();

        if (index == -1) {
            System.out.println("No MIDI devices available; skipping testSetOutputDeviceDoesNotThrow");
            return;
        }

        assertDoesNotThrow(() -> server.getMidiDeviceManager().trySetOutputDevice(index));
    }

    @Test
    void testSetInputDeviceDoesNotThrow() {
        MidiServer server = createServer();
        int index = getFirstAvailableDeviceIndex();

        if (index == -1) {
            System.out.println("No MIDI devices available; skipping testSetInputDeviceDoesNotThrow");
            return;
        }

        assertDoesNotThrow(() -> server.getMidiDeviceManager().trySetInputDevice(index));
    }

    @Test
    public void testClearBuffer(){
        MidiServer server = createServer();
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
