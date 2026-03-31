package MidiControl.functional.ContextModel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.sound.midi.SysexMessage;

import org.junit.jupiter.api.Test;

import MidiControl.ContextModel.Context;
import MidiControl.ContextModel.ContextDiscoveryEngine;
import MidiControl.ContextModel.ContextType;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Mocks.MockMidiIOManager;
import MidiControl.Mocks.MockMidiServer;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;
import MidiControl.UserInterface.ChannelName.ChannelNameAssembler;

public class NameAssemblerFunctionalm7clTest {

    private byte[] buildNameSysex(int subControlindex, int channel, int ascii, int ascii2) {

        return new byte[] {
            (byte)0xF0,
            (byte)0x43,
            (byte)0x10,                 // "1n" for MIDI device/channel
            62,
            17,
            1,
            1,
            19,
            0,
            (byte)(subControlindex),    // offset selects long-name position
            0,
            (byte)(channel),            // "cc" → CHANNEL INDEX
            0, 0, 0, (byte)ascii,                  // remaining dd dd dd
            (byte)(ascii2),              // ascii chr      
            (byte)0xF7
        };
    }

    // @Test
    public void testShortNameEndToEnd() throws Exception {

        List<SysexMapping> mappings =
            SysexMappingLoader.loadMappingsFromResource("MidiControl/m7cl_sysex_mappings.json");

        SysexParser parser = new SysexParser(mappings);
        CanonicalRegistry registry = new CanonicalRegistry(mappings, parser);

        MockMidiServer server = new MockMidiServer(registry);
        MockMidiIOManager io = server.getMockIo();

        ContextDiscoveryEngine engine = new ContextDiscoveryEngine(registry);

        List<Context> contexts = engine.discoverContexts();

        Context nameContext = contexts.stream()
            .filter(c -> c.getContextType() == ContextType.NAME)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No NAME context"));

        AtomicReference<String> lastName = new AtomicReference<>();

        new ChannelNameAssembler(nameContext, registry, (id, name) -> lastName.set(name));

        SysexMessage sysex = new SysexMessage();
        SysexMessage sysex1 = new SysexMessage();

        int messageLength = buildNameSysex(0, 0, 'K','i').length;

        sysex.setMessage(buildNameSysex(0, 0, 'K', 'i'),messageLength);
        server.addtoinputqueue(sysex);

        sysex1.setMessage(buildNameSysex(1, 0, 'c','k'),messageLength);
        server.addtoinputqueue(sysex1);
        server.processIncomingMidiForTest();
        Thread.sleep(1100);
        assertEquals("Kick", lastName.get());

    }


    // @Test
    // public void testAssemblers_doNotMixChannelNames() throws Exception {

    //     // Load mappings
    //     List<SysexMapping> mappings =
    //         SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");

    //     SysexParser parser = new SysexParser(mappings);
    //     CanonicalRegistry registry = new CanonicalRegistry(mappings, parser);

    //     // Server must come BEFORE assemblers
    //     MockMidiServer server = new MockMidiServer(registry);

    //     // Discover contexts
    //     ContextDiscoveryEngine engine = new ContextDiscoveryEngine(registry);
    //     List<Context> contexts = engine.discoverContexts();

    //     // We need ALL name contexts
    //     List<Context> nameContexts = contexts.stream()
    //         .filter(c -> c.getContextType() == ContextType.NAME)
    //         .toList();

    //     // Listener that captures all (contextId, name) updates
    //     Map<String, String> emittedNames = new HashMap<>();

    //     // One assembler per context
    //     for (Context c : nameContexts) {
    //         new ChannelNameAssembler(c, registry,
    //             (id, name) -> emittedNames.put(id, name));
    //     }

    //     int len = buildNameSysex(0, 0, 'K').length;

    //     // Channel 0 → Kick
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(0, 0, 'K'), len));
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(1, 0, 'i'), len));
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(2, 0, 'c'), len));
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(3, 0, 'k'), len));

    //     // Channel 1 → Snr
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(0, 1, 'S'), len));
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(1, 1, 'n'), len));
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(2, 1, 'r'), len));
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(3, 1, ' '), len));

    //     server.processIncomingMidiForTest();
    //     Thread.sleep(900);

    //     // Now assert routed values
    //     assertEquals("Kick", emittedNames.get("name.0"), "Channel 0 should assemble Kick");
    //     assertEquals("Snr",  emittedNames.get("name.1"), "Channel 1 should assemble Snr");

    //     assertNotEquals(
    //         emittedNames.get("name.0"),
    //         emittedNames.get("name.1"),
    //         "Channel names must be different"
    //     );
    // }

    // @Test
    // public void testOutOfOrderCharacterUpdates() throws Exception {
    //     List<SysexMapping> mappings =
    //         SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");

    //     SysexParser parser = new SysexParser(mappings);
    //     CanonicalRegistry registry = new CanonicalRegistry(mappings, parser);
    //     MockMidiServer server = new MockMidiServer(registry);

    //     ContextDiscoveryEngine engine = new ContextDiscoveryEngine(registry);
    //     Context ctx0 = engine.discoverContexts().stream()
    //         .filter(c -> c.getId().equals("name.0"))
    //         .findFirst()
    //         .orElseThrow();

    //     AtomicReference<String> result = new AtomicReference<>();
    //     new ChannelNameAssembler(ctx0, registry, (id, name) -> result.set(name));

    //     int len = buildNameSysex(0, 0, 'K').length;

    //     // Send updates OUT OF ORDER:
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(3, 0, 'k'), len)); // pos4
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(1, 0, 'i'), len)); // pos2
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(2, 0, 'c'), len)); // pos3
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(0, 0, 'K'), len)); // pos1

    //     server.processIncomingMidiForTest();
    //     Thread.sleep(900);

    //     assertEquals("Kick", result.get(),
    //         "Out-of-order updates must still assemble Kick");
    // }

    
    // @Test
    // public void testMissingCharactersAreHandled() throws Exception {
    //     List<SysexMapping> mappings =
    //         SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");

    //     SysexParser parser = new SysexParser(mappings);
    //     CanonicalRegistry registry = new CanonicalRegistry(mappings, parser);
    //     MockMidiServer server = new MockMidiServer(registry);

    //     Context ctx0 = new ContextDiscoveryEngine(registry)
    //         .discoverContexts().stream()
    //         .filter(c -> c.getId().equals("name.0"))
    //         .findFirst()
    //         .orElseThrow();

    //     AtomicReference<String> result = new AtomicReference<>();
    //     new ChannelNameAssembler(ctx0, registry, (id, name) -> result.set(name));

    //     int len = buildNameSysex(0, 0, 'K').length;

    //     // Missing “Short2” deliberately
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(0, 0, 'K'), len));
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(2, 0, 'c'), len)); // Short3
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(3, 0, 'k'), len)); // Short4

    //     server.processIncomingMidiForTest();
    //     Thread.sleep(900);

    //     // Expected: "K ck" or "Kck" depending on trimming rules
    //     assertEquals("Kck", result.get().replace(" ", ""),
    //         "Missing characters should not break assembly");
    // }

    
    // @Test
    // public void testEditingOverwritesPreviousCharacters() throws Exception {
    //     List<SysexMapping> mappings =
    //         SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");

    //     SysexParser parser = new SysexParser(mappings);
    //     CanonicalRegistry registry = new CanonicalRegistry(mappings, parser);
    //     MockMidiServer server = new MockMidiServer(registry);

    //     Context ctx0 = new ContextDiscoveryEngine(registry)
    //         .discoverContexts().stream()
    //         .filter(c -> c.getId().equals("name.0"))
    //         .findFirst()
    //         .orElseThrow();

    //     AtomicReference<String> result = new AtomicReference<>();
    //     new ChannelNameAssembler(ctx0, registry, (id, name) -> result.set(name));

    //     int len = buildNameSysex(0, 0, 'K').length;

    //     // Initial name: "Kick"
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(0, 0, 'K'), len));
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(1, 0, 'i'), len));
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(2, 0, 'c'), len));
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(3, 0, 'k'), len));

    //     // User edits name → "Fook"
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(0, 0, 'F'), len));
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(1, 0, 'o'), len));

    //     server.processIncomingMidiForTest();
    //     Thread.sleep(900);

    //     assertEquals("Fock", result.get(),
    //         "Editing must overwrite older characters");
    // }

    
    // @Test
    // public void testInterleavedChannelUpdatesAreIsolated() throws Exception {
    //     List<SysexMapping> mappings =
    //         SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");

    //     SysexParser parser = new SysexParser(mappings);
    //     CanonicalRegistry registry = new CanonicalRegistry(mappings, parser);
    //     MockMidiServer server = new MockMidiServer(registry);

    //     List<Context> nameContexts = new ContextDiscoveryEngine(registry)
    //         .discoverContexts().stream()
    //         .filter(c -> c.getContextType() == ContextType.NAME)
    //         .toList();

    //     Map<String,String> results = new HashMap<>();
    //     for (Context c : nameContexts) {
    //         new ChannelNameAssembler(c, registry,
    //             (id,name)->results.put(id, name));
    //     }
        
    //     int len = buildNameSysex(0, 0, 'K').length;
    //     // Channel 0 and 1 interleaved
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(0, 0, 'K'), len)); // ch0
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(0, 1, 'S'), len)); // ch1
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(1, 0, 'i'), len)); // ch0
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(1, 1, 'n'), len)); // ch1
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(2, 0, 'c'), len)); // ch0
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(2, 1, 'r'), len)); // ch1
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(3, 0, 'k'), len)); // ch0
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(3, 1, ' '), len)); // ch1

    //     server.processIncomingMidiForTest();
    //     Thread.sleep(1100);

    //     assertEquals("Kick", results.get("name.0"));
    //     assertEquals("Snr",  results.get("name.1"));
    // }
    
    // @Test
    // public void testNonAsciiValuesAreBlanked() throws Exception {
    //     List<SysexMapping> mappings =
    //         SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");

    //     SysexParser parser = new SysexParser(mappings);
    //     CanonicalRegistry registry = new CanonicalRegistry(mappings, parser);
    //     MockMidiServer server = new MockMidiServer(registry);

    //     Context ctx0 = new ContextDiscoveryEngine(registry)
    //         .discoverContexts().stream()
    //         .filter(c -> c.getId().equals("name.0"))
    //         .findFirst()
    //         .orElseThrow();

    //     AtomicReference<String> result = new AtomicReference<>();
    //     new ChannelNameAssembler(ctx0, registry, (id,name)->result.set(name));

    //     int len = buildNameSysex(0, 0, 0xFF).length;

    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(0, 0, 0xFF), len)); // invalid
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(1, 0, 'i'), len));

    //     server.processIncomingMidiForTest();
    //     Thread.sleep(900);

    //     assertEquals(" i", result.get(),
    //         "Non-ASCII characters should be replaced with spaces");
    // }

    
    // @Test
    // public void testDebounceProducesSingleNotification() throws Exception {
    //     List<SysexMapping> mappings =
    //         SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");

    //     SysexParser parser = new SysexParser(mappings);
    //     CanonicalRegistry registry = new CanonicalRegistry(mappings, parser);
    //     MockMidiServer server = new MockMidiServer(registry);

    //     Context ctx0 = new ContextDiscoveryEngine(registry)
    //         .discoverContexts().stream()
    //         .filter(c -> c.getId().equals("name.0"))
    //         .findFirst()
    //         .orElseThrow();

    //     List<String> events = new ArrayList<>();
    //     new ChannelNameAssembler(ctx0, registry, (id,name)->events.add(name));

    //     int len = buildNameSysex(0, 0, 'K').length;

    //     // Burst of rapid updates (< debounce time)
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(0, 0, 'K'), len));
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(1, 0, 'i'), len));
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(2, 0, 'c'), len));
    //     server.addtoinputqueue(new SysexMessage(buildNameSysex(3, 0, 'k'), len));

    //     server.processIncomingMidiForTest();
    //     Thread.sleep(100);  // No output yet (debounce still active)

    //     assertTrue(events.isEmpty(),
    //         "Debounce must prevent early emission");

    //     Thread.sleep(900); // Now debounce should fire

    //     assertEquals(1, events.size(),
    //         "Only one assembled name should be emitted");
    //     assertEquals("Kick", events.get(0));
    // }
}
