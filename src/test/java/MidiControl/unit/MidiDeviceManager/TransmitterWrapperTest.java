package MidiControl.unit.MidiDeviceManager;

import MidiControl.MidiDeviceManager.TransmitterWrapper;
import MidiControl.MidiInputReceiver;

import org.junit.jupiter.api.Test;

import javax.sound.midi.*;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.*;

public class TransmitterWrapperTest {


    private static class MockTransmitter implements Transmitter {

        public Receiver receiver;
        public boolean closed = false;

        @Override
        public void setReceiver(Receiver receiver) {
            this.receiver = receiver;
        }

        @Override
        public Receiver getReceiver() {
            return receiver;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static class MockReceiver implements Receiver {
        public boolean closed = false;
        public MidiMessage lastMessage;

        @Override
        public void send(MidiMessage message, long timeStamp) {
            lastMessage = message;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static class MockMidiDevice implements MidiDevice {

        private boolean open = false;
        private final Info info = new Info("MockDevice", "Test", "UnitTest", "1.0") {};
        private final MockTransmitter transmitter = new MockTransmitter();

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
        public Transmitter getTransmitter() {
            return transmitter;
        }

        // Unused methods
        @Override public int getMaxReceivers() { return 0; }
        @Override public Receiver getReceiver() { return null; }
        @Override public int getMaxTransmitters() { return 1; }
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
    public void testSetupOpensDeviceAndAttachesReceiver() throws Exception {
        MockMidiDevice device = new MockMidiDevice();
        ConcurrentLinkedQueue<MidiMessage> buffer = new ConcurrentLinkedQueue<>();

        TransmitterWrapper wrapper = new TransmitterWrapper(device, buffer);

        assertTrue(device.isOpen(), "Device should be opened");
        assertNotNull(wrapper.getRawTransmitter(), "Transmitter should be created");
        assertTrue(wrapper.getRawTransmitter().getReceiver() instanceof MidiInputReceiver,
                "Receiver should be a MidiInputReceiver");
    }

    public void testIncomingMidiMessagesGoToBuffer() throws Exception {
        MockMidiDevice device = new MockMidiDevice();
        ConcurrentLinkedQueue<MidiMessage> buffer = new ConcurrentLinkedQueue<>();

        TransmitterWrapper wrapper = new TransmitterWrapper(device, buffer);

        // Simulate incoming MIDI
        ShortMessage msg = new ShortMessage();
        msg.setMessage(0x90, 60, 100);

        device.getTransmitter().getReceiver().send(msg, -1);

        assertEquals(1, buffer.size(), "Buffer should contain one message");
        assertEquals(msg, buffer.peek(), "Message should match the one sent");
    }

    @Test
    public void testSetReceiverReplacesReceiver() throws Exception {
        MockMidiDevice device = new MockMidiDevice();
        ConcurrentLinkedQueue<MidiMessage> buffer = new ConcurrentLinkedQueue<>();

        TransmitterWrapper wrapper = new TransmitterWrapper(device, buffer);

        MockReceiver newReceiver = new MockReceiver();
        wrapper.setReceiver(newReceiver);

        assertSame(newReceiver, device.getTransmitter().getReceiver(),
                "Receiver should be replaced by setReceiver()");
    }

    @Test
    public void testCloseClosesEverything() throws Exception {
        MockMidiDevice device = new MockMidiDevice();
        ConcurrentLinkedQueue<MidiMessage> buffer = new ConcurrentLinkedQueue<>();

        TransmitterWrapper wrapper = new TransmitterWrapper(device, buffer);

        MockTransmitter tx = (MockTransmitter) device.getTransmitter();
        MidiInputReceiver inputReceiver = (MidiInputReceiver) tx.getReceiver();

        wrapper.close();

        assertTrue(tx.closed, "Transmitter should be closed");
        assertFalse(device.isOpen(), "Device should be closed");
        assertFalse(inputReceiver.isOpen(), "InputReceiver should be closed");
    }

    @Test
    public void testGetDeviceInfo() throws Exception {
        MockMidiDevice device = new MockMidiDevice();
        ConcurrentLinkedQueue<MidiMessage> buffer = new ConcurrentLinkedQueue<>();

        TransmitterWrapper wrapper = new TransmitterWrapper(device, buffer);

        assertEquals(device.getDeviceInfo(), wrapper.getDeviceInfo());
    }
}