package MidiControl.unit.UserInterface.Codec;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import MidiControl.ContextModel.Context;
import MidiControl.ContextModel.ContextFilter;
import MidiControl.ContextModel.ContextType;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.ControlInstance;
import MidiControl.Controls.SubControl;
import MidiControl.Mocks.MockCanonicalRegistry;
import MidiControl.UserInterface.ChannelName.ChannelNameAssembler;
import MidiControl.UserInterface.ChannelName.Codecs.ChannelNameCodec;
import MidiControl.UserInterface.ChannelName.Codecs.PerByteCodec;

public class ChannelNameAssemblerTriggerTest {

    private ControlInstance makeControl(MockCanonicalRegistry reg,
                                        String group,
                                        String sub,
                                        int index) {

        ControlGroup cg = reg.getGroup(group);

        if (cg == null) {
            cg = new ControlGroup(group);
            reg.getGroups().put(group, cg);
        }

        SubControl sc = cg.getSubcontrol(sub);

        if (sc == null) {
            sc = new SubControl(cg, sub);
            cg.getSubcontrols().put(sub, sc);
        }

        ControlInstance ci = new ControlInstance(sc, index, null, null);

        while (sc.getInstances().size() <= index) {
            sc.getInstances().add(null);
        }

        sc.getInstances().set(index, ci);

        return ci;
    }

    private Context makeNameContext() {
        return new Context(
            "name.0",
            "Name 1",
            ContextType.NAME,
            List.of(),
            List.of(
                new ContextFilter("kInputChannelName", "NameShort0", 0),
                new ContextFilter("kInputChannelName", "NameShort1", 0),
                new ContextFilter("kInputChannelName", "NameShort2", 0),
                new ContextFilter("kInputChannelName", "NameShort3", 0)
            )
        );
    }

    @Test
    public void assemblerPassesOrderedIntegerValuesToCodec() throws Exception {
        MockCanonicalRegistry reg = new MockCanonicalRegistry();

        ControlInstance s0 = makeControl(reg, "kInputChannelName", "NameShort0", 0);
        ControlInstance s1 = makeControl(reg, "kInputChannelName", "NameShort1", 0);
        ControlInstance s2 = makeControl(reg, "kInputChannelName", "NameShort2", 0);
        ControlInstance s3 = makeControl(reg, "kInputChannelName", "NameShort3", 0);

        Context ctx = makeNameContext();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<List<Integer>> codecInput = new AtomicReference<>();
        AtomicReference<String> emitted = new AtomicReference<>();

        ChannelNameCodec spyCodec = new ChannelNameCodec() {
            @Override
            public String decode(List<Integer> values) {
                codecInput.set(new ArrayList<>(values));
                return "Kick";
            }

            @Override
            public Optional<List<byte[]>> encode(String name) {
                return Optional.empty();
            }
        };

        ChannelNameAssembler assembler = new ChannelNameAssembler(
            ctx,
            reg,
            (id, name) -> {
                emitted.set(name);
                latch.countDown();
            },
            spyCodec
        );

        try {
            /*
             * Deliberately update out of order.
             * The assembler should still pass values ordered by subcontrol suffix:
             * NameShort0, NameShort1, NameShort2, NameShort3.
             */
            s3.updateValue('k');
            s1.updateValue('i');
            s2.updateValue('c');
            s0.updateValue('K');

            assertTrue(latch.await(2, TimeUnit.SECONDS));

            assertEquals("Kick", emitted.get());

            assertEquals(
                List.of((int) 'K', (int) 'i', (int) 'c', (int) 'k'),
                codecInput.get()
            );
        } finally {
            assembler.shutdown();
        }
    }

    @Test
    public void assemblerWithPerByteCodecBuildsKick() throws Exception {
        MockCanonicalRegistry reg = new MockCanonicalRegistry();

        ControlInstance s0 = makeControl(reg, "kInputChannelName", "NameShort0", 0);
        ControlInstance s1 = makeControl(reg, "kInputChannelName", "NameShort1", 0);
        ControlInstance s2 = makeControl(reg, "kInputChannelName", "NameShort2", 0);
        ControlInstance s3 = makeControl(reg, "kInputChannelName", "NameShort3", 0);

        Context ctx = makeNameContext();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>();

        ChannelNameAssembler assembler = new ChannelNameAssembler(
            ctx,
            reg,
            (id, name) -> {
                result.set(name);
                latch.countDown();
            },
            new PerByteCodec()
        );

        try {
            s0.updateValue('K');
            s1.updateValue('i');
            s2.updateValue('c');
            s3.updateValue('k');

            assertTrue(latch.await(2, TimeUnit.SECONDS));

            assertEquals("Kick", result.get());
        } finally {
            assembler.shutdown();
        }
    }

    @Test
    public void assemblerDebouncesMultipleUpdatesIntoSingleEmission() throws Exception {
        MockCanonicalRegistry reg = new MockCanonicalRegistry();

        ControlInstance s0 = makeControl(reg, "kInputChannelName", "NameShort0", 0);
        ControlInstance s1 = makeControl(reg, "kInputChannelName", "NameShort1", 0);
        ControlInstance s2 = makeControl(reg, "kInputChannelName", "NameShort2", 0);
        ControlInstance s3 = makeControl(reg, "kInputChannelName", "NameShort3", 0);

        Context ctx = makeNameContext();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger emissions = new AtomicInteger(0);
        AtomicReference<String> result = new AtomicReference<>();

        ChannelNameAssembler assembler = new ChannelNameAssembler(
            ctx,
            reg,
            (id, name) -> {
                emissions.incrementAndGet();
                result.set(name);
                latch.countDown();
            },
            new PerByteCodec()
        );

        try {
            s0.updateValue('K');
            s1.updateValue('i');
            s2.updateValue('c');
            s3.updateValue('k');

            assertTrue(latch.await(2, TimeUnit.SECONDS));

            assertEquals("Kick", result.get());
            assertEquals(1, emissions.get());
        } finally {
            assembler.shutdown();
        }
    }
}
