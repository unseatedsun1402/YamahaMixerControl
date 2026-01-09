package MidiControl.unit.MidiDeviceManager;

import MidiControl.MidiDeviceManager.ReceiverWrapper;
import org.junit.jupiter.api.Test;

import javax.sound.midi.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReceiverWrapperTest {

    private static class MockReceiver implements Receiver {

        public final List<MidiMessage> received = new ArrayList<>();
        public boolean closed = false;

        @Override
        public void send(MidiMessage message, long timeStamp) {
            received.add(message);
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static class MockMidiDevice implements MidiDevice {

        private final Info info = new Info("MockDevice", "Test", "UnitTest", "1.0") {};
        private boolean open = false;
        private final MockReceiver receiver = new MockReceiver();

        @Override
        public Info getDeviceInfo() {
            return info;
        }

        @Override
        public void open() {
            open = true;
        }

        @Override
        public void close() {
            open = false;
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public int getMaxReceivers() {
            return 1;
        }

        @Override
        public Receiver getReceiver() {
            return receiver;
        }

        // Unused methods
        @Override public int getMaxTransmitters() { return 0; }
        @Override public Transmitter getTransmitter() { return null; }
        @Override public long getMicrosecondPosition() { return 0; }

        @Override
        public List<Receiver> getReceivers() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getReceivers'");
        }

        @Override
        public List<Transmitter> getTransmitters() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getTransmitters'");
        }
    }

    @Test
    public void testSendSysEx() throws Exception {
        MockMidiDevice device = new MockMidiDevice();
        ReceiverWrapper wrapper = new ReceiverWrapper(device);

        byte[] sysex = new byte[] { (byte)0xF0, 0x01, 0x02, (byte)0xF7 };

        wrapper.sendMessage(sysex);

        assertEquals(1, device.receiver.received.size());
        assertTrue(device.receiver.received.get(0) instanceof SysexMessage);

        SysexMessage msg = (SysexMessage) device.receiver.received.get(0);
        assertArrayEquals(sysex, msg.getMessage());
    }

    @Test
    public void testSendShortMessage() throws Exception {
        MockMidiDevice device = new MockMidiDevice();
        ReceiverWrapper wrapper = new ReceiverWrapper(device);

        byte[] cc = new byte[] { (byte)0xB0, 0x10, 0x7F }; // CC 16 = 127

        wrapper.sendMessage(cc);

        assertEquals(1, device.receiver.received.size());
        assertTrue(device.receiver.received.get(0) instanceof ShortMessage);

        ShortMessage msg = (ShortMessage) device.receiver.received.get(0);
        assertEquals(0xB0, msg.getStatus());
        assertEquals(0x10, msg.getData1());
        assertEquals(0x7F, msg.getData2());
    }


    @Test
    public void testInvalidMessageLength() throws Exception {
        MockMidiDevice device = new MockMidiDevice();
        ReceiverWrapper wrapper = new ReceiverWrapper(device);

        wrapper.sendMessage(new byte[] { 0x01 }); // too short

        assertEquals(0, device.receiver.received.size());
    }

    @Test
    public void testLegacySendMessage() throws Exception {
        MockMidiDevice device = new MockMidiDevice();
        ReceiverWrapper wrapper = new ReceiverWrapper(device);

        ShortMessage sm = new ShortMessage();
        sm.setMessage(0x90, 60, 100); // Note on

        wrapper.sendMessage(sm);

        assertEquals(1, device.receiver.received.size());
        assertEquals(sm, device.receiver.received.get(0));
    }

    @Test
    public void testOpenClose() throws Exception {
        MockMidiDevice device = new MockMidiDevice();
        ReceiverWrapper wrapper = new ReceiverWrapper(device);

        assertTrue(wrapper.isOpen());
        assertTrue(device.isOpen());

        wrapper.close();

        assertFalse(device.isOpen());
        assertTrue(device.receiver.closed);
    }

    @Test
    public void testGetRawReceiver() throws Exception {
        MockMidiDevice device = new MockMidiDevice();
        ReceiverWrapper wrapper = new ReceiverWrapper(device);

        assertNotNull(wrapper.getRawReceiver());
        assertSame(device.receiver, wrapper.getRawReceiver());
    }
}