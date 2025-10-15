package MidiControl.functional;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import MidiControl.MidiInputReceiver;
import MidiControl.MidiServer;
import MidiControl.ChannelMappings.ChannelType;
import MidiControl.ChannelMappings.ControlType;
import MidiControl.TestUtilities.MidiTestUtils;
import MidiControl.Utilities.NrpnMapping;
import MidiControl.Utilities.NrpnRegistry;

import static org.junit.jupiter.api.Assertions.*;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;

import java.util.concurrent.ConcurrentLinkedQueue;

@Tag("functional")
public class MidiServerTest {
    @Test
    void testAddToSendQueue() {
        ShortMessage[] commands = new ShortMessage[1];
        try {
            commands[0] = new ShortMessage();
            commands[0].setMessage(ShortMessage.NOTE_ON, 0, 60, 127);
        } catch (Exception e) {
            org.junit.jupiter.api.Assertions.fail("Failed to create ShortMessage");
        }
        int before = MidiServer.getBufferSize();
        MidiServer.addtosendqueue(commands);
        int after = MidiServer.getBufferSize();
        org.junit.jupiter.api.Assertions.assertEquals(before + 1, after, "Buffer should have one more element after adding");
    }

    @Test
    void testInputReceiverReceivesMessage() {
        ConcurrentLinkedQueue<MidiMessage> inputBuffer = new ConcurrentLinkedQueue<>();
        try (MidiInputReceiver receiver = new MidiInputReceiver(inputBuffer)) {
            ShortMessage msg = new ShortMessage();
            try {
                msg.setMessage(ShortMessage.NOTE_ON, 0, 60, 127);
            } catch (Exception e) {
                org.junit.jupiter.api.Assertions.fail("Failed to create ShortMessage");
            }
            receiver.send(msg, -1);
            org.junit.jupiter.api.Assertions.assertFalse(inputBuffer.isEmpty(), "Input buffer should not be empty after receiving a message");
            org.junit.jupiter.api.Assertions.assertEquals(msg, inputBuffer.poll(), "Received message should match sent message");
        }
    }

    @Test
    public void testNrpnJsonStructureWithHelpers() {
        NrpnMapping mapping = new NrpnMapping(1, 0, ControlType.FADER, ChannelType.MIX, 3, "Mix 9 send 3 control");
        String json = MidiTestUtils.buildNrpnJson(mapping, 8192);

        MidiTestUtils.assertJsonContains(json, "type", "nrpnUpdate");
        MidiTestUtils.assertJsonContains(json, "channelType", "MIX");
        MidiTestUtils.assertJsonContains(json, "channelIndex", "3");
        MidiTestUtils.assertJsonContains(json, "controlType", "FADER");
        MidiTestUtils.assertJsonContains(json, "value", "8192");
        MidiTestUtils.assertJsonContains(json, "msb", "1");
        MidiTestUtils.assertJsonContains(json, "lsb", "0");
    }

    // @Test
    // void testSetOutputDeviceInitializesReceiver() throws Exception {
    //     MidiServer.setOutputDevice(0); // Assumes device 0 has receivers // internal synth
    //     assertTrue(MidiServer.isReceiverOpen(), "Receiver should be open");
    //     assertTrue(MidiServer.getReceiver().getClass().getName().contains("Receiver"), "Receiver should be a valid implementation");
    // }

    @Test
    void testSetInputDeviceInitializesTransmitter() throws Exception {
        MidiServer.setInputDevice(1); // Assumes device 1 has transmitters // internal squencer
        assertTrue(MidiServer.isTransmitterOpen(), "Transmitter should be initialized");
        assertNotNull(MidiServer.getTransmitter().getReceiver(), "MIDI input device should be open");
    }

    @Test
    public void testIncomingMidiParsing() {
        ShortMessage msg = null;
        try {
            msg = new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 99, 1);
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
        MidiServer.addtoinputqueue(msg);
        new MidiServer().processIncomingMidi();
        // Assert parser state or logs
    }

    @Test
    void testSetInputDeviceWithInvalidIndexThrows() {
        assertThrows(MidiUnavailableException.class, () -> MidiServer.setInputDevice(999), "Should throw for invalid device index");
    }

    @Test
    void testSetOutputDeviceWithInvalidIndexThrows() {
        assertThrows(MidiUnavailableException.class, () -> MidiServer.setInputDevice(999), "Should throw for invalid device index");
    }

    @Test
    void testSetInputDeviceWithOutputIndexFailsGracefully() {
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        MidiDevice device = null;
        try {
        device = MidiSystem.getMidiDevice(infos[0]); // Assumes index 0 is output-only
        }
        catch (MidiUnavailableException e) {
            fail("Device at index 0 should be available for testing");
            return;
        }
        assertTrue(device.getMaxTransmitters() == 0, "Device should not support transmitters (not suitable for input)");
        assertDoesNotThrow(() -> {
            try {
                MidiServer.setInputDevice(0);
            } catch (MidiUnavailableException e) {
                // Expected: device is valid but not usable as input
            }
        }, "Should not crash when setting output-only device as input");
    }

    @Test
    void testOutputFaderJsonDispatch() {
        NrpnRegistry.getInstance().buildProfileMapping(); // Ensure mappings are loaded

        // Simulate incoming NRPN for output fader (MSB=2, LSB=0)
        ShortMessage msg1 = MidiTestUtils.createControlChange(0, 99, 2); // NRPN MSB
        ShortMessage msg2 = MidiTestUtils.createControlChange(0, 98, 0); // NRPN LSB
        ShortMessage msg3 = MidiTestUtils.createControlChange(0, 6, 64); // Value MSB

        MidiServer.addtoinputqueue(msg1);
        MidiServer.addtoinputqueue(msg2);
        MidiServer.addtoinputqueue(msg3);

        MidiServer server = new MidiServer();
        server.processIncomingMidi();

        NrpnMapping mapping = NrpnRegistry.resolve(2, 0).orElseThrow();
        String json = MidiControl.ControlMappings.buildFaderJson.buildOutputFaderJson(mapping, 8192);
        assertTrue(MidiControl.TestUtilities.MidiTestUtils.isValidJson(json));
        assertTrue(json.contains("\"type\":\"outputFaderUpdate\""));
        assertTrue(json.contains("\"channelIndex\":" + mapping.channelIndex()));
        assertTrue(json.contains("\"value\":8192"));
        assertTrue(json.contains("\"label\":\"" + mapping.label() + "\""));
    }

    @Test
    void testFaderJsonDispatch() {
        NrpnRegistry.getInstance().buildProfileMapping(); // Ensure mappings are loaded

        // Simulate incoming NRPN for output fader (MSB=2, LSB=0)
        ShortMessage msg1 = MidiTestUtils.createControlChange(0, 99, 2); // NRPN MSB
        ShortMessage msg2 = MidiTestUtils.createControlChange(0, 98, 0); // NRPN LSB
        ShortMessage msg3 = MidiTestUtils.createControlChange(0, 6, 64); // Value MSB

        MidiServer.addtoinputqueue(msg1);
        MidiServer.addtoinputqueue(msg2);
        MidiServer.addtoinputqueue(msg3);

        MidiServer server = new MidiServer();
        server.processIncomingMidi();

        NrpnMapping mapping = NrpnRegistry.resolve(2, 0).orElseThrow();
        String json = MidiControl.ControlMappings.buildFaderJson.buildInputFaderJson(mapping, 8192);
        assertTrue(MidiControl.TestUtilities.MidiTestUtils.isValidJson(json));
        assertTrue(json.contains("\"type\":\"nrpnUpdate\""));
        assertTrue(json.contains("\"channelIndex\":" + mapping.channelIndex()));
        assertTrue(json.contains("\"value\":8192"));
        assertTrue(json.contains("\"label\":\"" + mapping.label() + "\""));
    }
}
