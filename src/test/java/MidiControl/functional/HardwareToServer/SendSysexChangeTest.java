package MidiControl.functional.HardwareToServer;

import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.TestUtilities.MidiTestUtils;
import MidiControl.UserInterface.Frontend.GuiBroadcaster;
import MidiControl.UserInterface.CanonicalContextResolver;
import MidiControl.UserInterface.Frontend.GuiBroadcastListener;
import MidiControl.Server.MidiServer;

import org.junit.jupiter.api.Test;

import javax.sound.midi.SysexMessage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SendSysexChangeTest {

    @Test
    void testSysExReception() throws Exception {

        // Capture broadcast output
        List<String> broadcasts = new ArrayList<>();
        GuiBroadcaster fakeBroadcaster = (json, ctx) -> broadcasts.add(json);

        CanonicalContextResolver fakeResolver = canonicalId -> "test.context";

        // Build registry
        List<SysexMapping> mappings = SysexMappingLoader.loadAllMappings();
        SysexParser parser = new SysexParser(mappings);
        CanonicalRegistry registry = new CanonicalRegistry(mappings, parser);

        // Inject fake listener
        GuiBroadcastListener listener =
            new GuiBroadcastListener(fakeBroadcaster, fakeResolver);

        MidiServer server = new MidiServer(registry);
        server.setGuiBroadcastListener(listener);

        // Fake incoming sysex
        byte[] faderMsg = {
            (byte) 0xF0, (byte) 0x43, (byte) 0x10, (byte) 0x3E,
            (byte) 0x7F, (byte) 0x01, (byte) 0x1C, (byte) 0x00,
            (byte) 0x1A, (byte) 0x00, (byte) 0x00, (byte) 0x03,
            (byte) 0x14, (byte) 0xF7
        };

        SysexMessage msg = MidiTestUtils.createSysexMessage(faderMsg);

        // Feed into server
        server.addtoinputqueue(msg);
        server.processIncomingMidiForTest();

        // Assertions
        assertFalse(broadcasts.isEmpty(), "Expected at least one broadcast message");

        System.out.println("Broadcast JSON: " + broadcasts.get(0));
    }
}