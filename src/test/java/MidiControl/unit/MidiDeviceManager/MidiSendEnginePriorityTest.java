package MidiControl.unit.MidiDeviceManager;

import MidiControl.MidiDeviceManager.MidiOutput;
import MidiControl.MidiDeviceManager.MidiSendEngine;
import org.junit.jupiter.api.Test;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MidiSendEnginePriorityTest {

    @Test
    void priorityMessagesPassQueuedNormalMessagesButFollowRealtimeMessages() throws Exception {
        CountDownLatch firstMessageStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstMessage = new CountDownLatch(1);
        CountDownLatch allMessagesSent = new CountDownLatch(5);
        List<Integer> sentStatuses = new ArrayList<>();

        MidiOutput output = new MidiOutput() {
            @Override
            public void sendMessage(byte[] message) {
                synchronized (sentStatuses) {
                    sentStatuses.add(message[0] & 0xFF);
                }
                if ((message[0] & 0xFF) == 0x10) {
                    firstMessageStarted.countDown();
                    try {
                        if (!releaseFirstMessage.await(2, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Test did not release the first MIDI message");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("Interrupted while holding the first MIDI message", e);
                    }
                }
                allMessagesSent.countDown();
            }

            @Override
            public void sendMessage(MidiMessage message) {
                throw new UnsupportedOperationException();
            }

            @Override
            public MidiDevice.Info getDeviceInfo() {
                return null;
            }

            @Override
            public boolean isOpen() {
                return true;
            }

            @Override
            public void close() {}
        };

        MidiSendEngine engine = new MidiSendEngine(output, 8, 8);
        try {
            engine.start();
            assertTrue(engine.offer(new byte[]{0x10}));
            assertTrue(firstMessageStarted.await(2, TimeUnit.SECONDS));

            assertTrue(engine.offer(new byte[]{0x20}));
            assertTrue(engine.offer(new byte[]{0x21}));
            assertTrue(engine.offerPriority(new byte[]{0x30}));
            assertTrue(engine.offerPriority(new byte[]{(byte) 0xF8}));

            releaseFirstMessage.countDown();
            assertTrue(allMessagesSent.await(2, TimeUnit.SECONDS));

            synchronized (sentStatuses) {
                assertEquals(List.of(0x10, 0xF8, 0x30, 0x20, 0x21), sentStatuses);
            }
        } finally {
            releaseFirstMessage.countDown();
            engine.stop();
        }
    }
}
