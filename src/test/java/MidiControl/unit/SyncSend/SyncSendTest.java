package MidiControl.unit.SyncSend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import org.junit.jupiter.api.Test;

import MidiControl.SyncSend;
import MidiControl.Mocks.MockMidiIOManager;
import MidiControl.Mocks.MockMidiOut;
import MidiControl.Mocks.MockMidiServer;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.SysexUtils.SysexParser;
import MidiControl.SysexUtils.SysexMapping;

import java.util.ArrayList;
import java.util.List;

public class SyncSendTest {

    private CanonicalRegistry makeMinimalRegistry() {
        List<SysexMapping> sysex = new ArrayList<>();
        SysexParser parser = new SysexParser(sysex);
        return new CanonicalRegistry(sysex, parser);
    }

    private MockMidiIOManager makeIo(MockMidiOut out) {
        CanonicalRegistry registry = makeMinimalRegistry();
        MockMidiServer server = new MockMidiServer(registry);
        MockMidiIOManager io = new MockMidiIOManager(server);
        io.setMidiOutForTest(out);
        server.setMockIo(io);
        return io;
    }

    @Test
    void testSyncSendSendsMessages() throws Exception {
        ConcurrentLinkedQueue<MidiMessage[]> buffer = new ConcurrentLinkedQueue<>();

        MidiMessage msg1 = new ShortMessage(0x90, 0x40, 0x7F);
        MidiMessage msg2 = new ShortMessage(0x80, 0x40, 0x00);

        buffer.add(new MidiMessage[]{msg1, msg2});

        MockMidiOut out = new MockMidiOut();
        MockMidiIOManager io = makeIo(out);

        SyncSend sync = new SyncSend(buffer, io);
        Thread t = new Thread(sync);

        t.start();
        long timeout = System.currentTimeMillis() + 200;
        while (!buffer.isEmpty() && System.currentTimeMillis() < timeout) {
            Thread.sleep(1);
        }
        t.interrupt();
        t.join(100);

        assertEquals(2, out.sent.size());
    }

    @Test
    void testSyncSendExitsOnEmptyMessages() throws Exception {
        ConcurrentLinkedQueue<MidiMessage[]> buffer = new ConcurrentLinkedQueue<>();

        MidiMessage msg1 = new ShortMessage(0x80, 0x40, 0x00);
        MidiMessage msg2 = MidiControl.TestUtilities.MidiTestUtils.createSysexMessage(new byte[]{});

        buffer.add(new MidiMessage[]{msg1, msg2});

        MockMidiOut out = new MockMidiOut();
        MockMidiIOManager io = makeIo(out);

        SyncSend sync = new SyncSend(buffer, io);
        Thread t = new Thread(sync);

        t.start();
        long timeout = System.currentTimeMillis() + 200;
        while (!buffer.isEmpty() && System.currentTimeMillis() < timeout) {
            Thread.sleep(1);
        }
        t.interrupt();
        t.join(100);

        assertEquals(1, out.sent.size());
    }

    @Test
    void testSyncSendIsInterrupted() throws Exception {
        ConcurrentLinkedQueue<MidiMessage[]> buffer = new ConcurrentLinkedQueue<>();

        MidiMessage msg1 = new ShortMessage(0x80, 0x40, 0x00);
        MidiMessage msg2 = new ShortMessage(0x80, 0x40, 0x00);

        buffer.add(new MidiMessage[]{msg1});

        MockMidiOut out = new MockMidiOut();
        MockMidiIOManager io = makeIo(out);

        SyncSend sync = new SyncSend(buffer, io);
        Thread t = new Thread(sync);

        t.start();
        long timeout = System.currentTimeMillis() + 200;
        while (out.sent.size() < 1 && System.currentTimeMillis() < timeout) {
            Thread.sleep(1);
        }

        // Interrupt before adding the second batch
        t.interrupt();

        // Add second batch AFTER interrupt
        buffer.add(new MidiMessage[]{msg2});
        t.join(100);

        // Only the first message should have been sent
        assertEquals(1, out.sent.size());

        // Second batch should still be in the buffer
        assertEquals(1, buffer.size());
    }

    @Test
    void testSyncSendIdleBranch() throws Exception {
        ConcurrentLinkedQueue<MidiMessage[]> buffer = new ConcurrentLinkedQueue<>();

        MockMidiOut mockOut = new MockMidiOut();
        MockMidiIOManager io = makeIo(mockOut);

        SyncSend sync = new SyncSend(buffer, io);
        Thread t = new Thread(sync);

        t.start();

        // Give it time to hit the idle branch at least once
        Thread.sleep(20);

        t.interrupt();
        t.join(100);

        // No messages should have been sent
        assertTrue(mockOut.sent.isEmpty());
    }
}