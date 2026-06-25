package MidiControl.functional.UserInterface;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.sound.midi.SysexMessage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import MidiControl.ContextModel.Context;
import MidiControl.ContextModel.ContextDiscoveryEngine;
import MidiControl.ContextModel.ContextType;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Mocks.MockMidiServer;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;
import MidiControl.UserInterface.ChannelName.ChannelNameAssembler;
import MidiControl.UserInterface.ChannelName.Codecs.M7clNameCodec;

public class NameAssemblerFunctionalm7clTest {

    private static final String RESOURCE =
        "MidiControl/m7cl_sysex_mappings.json";

    private static final String INPUT_NAME_GROUP =
        "kNameInputChannel";

    private final List<ChannelNameAssembler> assemblers = new ArrayList<>();

    @AfterEach
    public void tearDown() {
        for (ChannelNameAssembler assembler : assemblers) {
            assembler.shutdown();
        }

        assemblers.clear();
    }

    private CanonicalRegistry buildRegistry() {
        List<SysexMapping> mappings =
            SysexMappingLoader.loadMappingsFromResource(RESOURCE);

        SysexParser parser = new SysexParser(mappings);
        CanonicalRegistry registry = new CanonicalRegistry(mappings, parser);
        registry.setDeskType("YAMAHA_M7CL");
        return registry;
    }

    private Context findM7clInputNameContext(CanonicalRegistry registry, int channel) {
        return new ContextDiscoveryEngine(registry)
            .discoverContexts()
            .stream()
            .filter(c -> c.getContextType() == ContextType.NAME)
            .filter(c -> c.getFilters().stream()
                .anyMatch(f ->
                    INPUT_NAME_GROUP.equals(f.getControlGroup()) &&
                    f.getIndex() == channel
                )
            )
            .findFirst()
            .orElseThrow(() ->
                new IllegalStateException("No M7CL input NAME context for channel " + channel));
    }

    private byte[] buildNameSysex(int subControlIndex, int channel, byte[] dd5) {
        if (dd5 == null || dd5.length != 5) {
            throw new IllegalArgumentException("M7CL name SysEx requires exactly five dd bytes");
        }

        return new byte[] {
            (byte) 0xF0,
            (byte) 0x43,
            (byte) 0x10,
            62,
            17,
            1,
            1,
            19,                         // decimal 19 = 0x13
            0,
            (byte) subControlIndex,      // 0 = Short1, 1 = Short2
            0,
            (byte) channel,              // cc cc
            dd5[0],
            dd5[1],
            dd5[2],
            dd5[3],
            dd5[4],
            (byte) 0xF7
        };
    }

    private void enqueueName(MockMidiServer server, int channel, String name)
        throws Exception {

        M7clNameCodec.EncodedName encoded =
            M7clNameCodec.encodeName(name);

        byte[] short1 = buildNameSysex(0, channel, encoded.short1());
        byte[] short2 = buildNameSysex(1, channel, encoded.short2());

        server.addtoinputqueue(new SysexMessage(short1, short1.length));
        server.addtoinputqueue(new SysexMessage(short2, short2.length));
    }

    @Test
    public void incomingResolvedMappingsEmitDecodedM7clNameWhenNotEmpty()
        throws Exception {

        CanonicalRegistry registry = buildRegistry();
        MockMidiServer server = new MockMidiServer(registry);

        Context context = findM7clInputNameContext(registry, 0);

        AtomicReference<String> lastName = new AtomicReference<>();

        ChannelNameAssembler assembler =
            new ChannelNameAssembler(context, registry, (id, name) -> lastName.set(name));

        assemblers.add(assembler);

        enqueueName(server, 0, "test1");

        server.processIncomingMidiForTest();
        Thread.sleep(1100);

        assertEquals("test1", lastName.get());
    }

    @Test
    public void incomingResolvedMappingsDecodeKnownM7clFullName()
        throws Exception {

        CanonicalRegistry registry = buildRegistry();
        MockMidiServer server = new MockMidiServer(registry);

        Context context = findM7clInputNameContext(registry, 0);

        AtomicReference<String> lastName = new AtomicReference<>();

        ChannelNameAssembler assembler =
            new ChannelNameAssembler(context, registry, (id, name) -> lastName.set(name));

        assemblers.add(assembler);

        enqueueName(server, 0, "SPX MIDI");

        server.processIncomingMidiForTest();
        Thread.sleep(1100);

        assertEquals("SPX MIDI", lastName.get());
    }

    @Test
    public void blankDecodedM7clNameDoesNotEmitUpdate()
        throws Exception {

        CanonicalRegistry registry = buildRegistry();
        MockMidiServer server = new MockMidiServer(registry);

        Context context = findM7clInputNameContext(registry, 0);

        AtomicReference<String> lastName = new AtomicReference<>();

        ChannelNameAssembler assembler =
            new ChannelNameAssembler(context, registry, (id, name) -> lastName.set(name));

        assemblers.add(assembler);

        enqueueName(server, 0, "");

        server.processIncomingMidiForTest();
        Thread.sleep(1100);

        assertNull(lastName.get(),
            "Blank M7CL names should not emit a UI update");
    }
}