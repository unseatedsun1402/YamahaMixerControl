package MidiControl.unit.UserInterface;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import MidiControl.ContextModel.Context;
import MidiControl.ContextModel.ContextFilter;
import MidiControl.ContextModel.ContextType;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.ControlInstance;
import MidiControl.Controls.SubControl;
import MidiControl.Mocks.MockCanonicalRegistry;
import MidiControl.UserInterface.ChannelName.ChannelNameAssembler;

public class NameAssemblerTest {
    private ChannelNameAssembler assembler;

    @AfterEach
    void shutDownAssembler() {
        if (assembler != null) {
            assembler.shutdown();
        }
    }

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

    @Test
    public void testChannelNameAssembler_UsesShortName() throws Exception {

        MockCanonicalRegistry reg = new MockCanonicalRegistry();

        // Use the correct group name: kInputChannelName
        ControlInstance S0 = makeControl(reg, "kInputChannelName", "NameShort0", 0);
        ControlInstance S1 = makeControl(reg, "kInputChannelName", "NameShort1", 0);
        ControlInstance S2 = makeControl(reg, "kInputChannelName", "NameShort2", 0);
        ControlInstance S3 = makeControl(reg, "kInputChannelName", "NameShort3", 0);

        Context ctx = new Context(
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

        AtomicReference<String> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        assembler = new ChannelNameAssembler(ctx, reg, (id, name) -> {
            result.set(name);
            latch.countDown();
        });

        // Update short-name values
        S0.updateValue('K');
        S1.updateValue('i');
        S2.updateValue('c');
        S3.updateValue('k');

        // Wait for assembler debounce
        latch.await(1, TimeUnit.SECONDS);

        assertEquals("Kick", result.get());
    }

    @Test
    public void testChannelNameAssembler_DCAName() throws Exception {

        MockCanonicalRegistry reg = new MockCanonicalRegistry();

        ControlInstance S0 = makeControl(reg, "kDCAName", "kNameShort1", 0);
        ControlInstance S1 = makeControl(reg, "kDCAName", "kNameShort2", 0);
        ControlInstance S2 = makeControl(reg, "kDCAName", "kNameShort3", 0);
        ControlInstance S3 = makeControl(reg, "kDCAName", "kNameShort4", 0);

        Context ctx = new Context(
            "name.dca.0",
            "DCA 1 Name",
            ContextType.NAME,
            List.of(),
            List.of(
                new ContextFilter("kDCAName", "kNameShort1", 0),
                new ContextFilter("kDCAName", "kNameShort2", 0),
                new ContextFilter("kDCAName", "kNameShort3", 0),
                new ContextFilter("kDCAName", "kNameShort4", 0)
            )
        );

        AtomicReference<String> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        assembler = new ChannelNameAssembler(ctx, reg, (id, name) -> {
            result.set(name);
            latch.countDown();
        });

        S0.updateValue('D');
        S1.updateValue('r');
        S2.updateValue('m');
        S3.updateValue('s');

        latch.await(1, TimeUnit.SECONDS);

        assertEquals("Drms", result.get());
    }
}
