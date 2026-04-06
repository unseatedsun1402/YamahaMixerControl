package MidiControl.functional.HardwareToServer;

import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;
import MidiControl.ControlServer.HardwareInputHandler;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.TestUtilities.MidiTestUtils;
import MidiControl.UserInterface.Frontend.GuiBroadcaster;
import MidiControl.UserInterface.Meter.MeterBroadcaster;
import MidiControl.UserInterface.CanonicalContextResolver;
import MidiControl.UserInterface.Frontend.GuiBroadcastListener;
import MidiControl.Server.MidiServer;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.sound.midi.SysexMessage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SendSysexChangeTest {

    @Test
    void testSysExReception01v96() throws Exception {

        // Capture broadcast output
        List<String> broadcasts = new ArrayList<>();
        GuiBroadcaster fakeBroadcaster = (json, ctx) -> broadcasts.add(json);

        CanonicalContextResolver fakeResolver = canonicalId -> "test.context";

        // Build registry based on 01v96i
        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");
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

        server.addtoinputqueue(msg);
        server.processIncomingMidiForTest();

        assertFalse(broadcasts.isEmpty(), "Expected at least one broadcast message");

        assertEquals(404, new Gson().
        fromJson(broadcasts.get(0), JsonObject.class).
        getAsJsonObject("payload").get("value").getAsInt());
    }

    @Test
    void testSysExReceptionm7cl() throws Exception {

        List<String> broadcasts = new ArrayList<>();
        GuiBroadcaster fakeBroadcaster = (json, ctx) -> broadcasts.add(json);

        CanonicalContextResolver fakeResolver = canonicalId -> "test.context";

        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromResource("MidiControl/m7cl_sysex_mappings.json");
        SysexParser parser = new SysexParser(mappings);
        CanonicalRegistry registry = new CanonicalRegistry(mappings, parser);

        GuiBroadcastListener listener =
            new GuiBroadcastListener(fakeBroadcaster, fakeResolver);

        MidiServer server = new MidiServer(registry);
        server.setGuiBroadcastListener(listener);

        byte[] faderMsg = {
            (byte) 0xF0, (byte) 0x43, (byte) 0x10, (byte) 0x3E,
            (byte) 0x11, (byte) 0x01, (byte) 0x0, (byte) 0x32,
            (byte) 0x0, (byte) 0x0, (byte) 0x00, (byte) 0x01,
            0x00,0x0,0x0,0x6,0x2,
            (byte) 0xF7
        };

        SysexMessage msg = MidiTestUtils.createSysexMessage(faderMsg);

        server.addtoinputqueue(msg);
        server.processIncomingMidiForTest();

        assertFalse(broadcasts.isEmpty(), "Expected at least one broadcast message");

        assertEquals(770, new Gson().
        fromJson(broadcasts.get(0), JsonObject.class).
        getAsJsonObject("payload").get("value").getAsInt());
    }

    @Test
    void testMeterBroadcast() throws Exception {

        List<String> broadcasts = new ArrayList<>();
        MeterBroadcaster fakeBroadcaster = new MeterBroadcaster(){
            @Override public void onMeterUpdate(MidiControl.UserInterface.Meter.MeterDTO dto) {
                broadcasts.add(dto.toJson());
            }
        };

        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");
        SysexParser parser = new SysexParser(mappings);
        CanonicalRegistry registry = new CanonicalRegistry(mappings, parser);

        MidiServer server = new MidiServer(registry);

        HardwareInputHandler.setMeterBroadcaster(fakeBroadcaster);

        byte[] faderMsg = {
            (byte) 0xF0, (byte) 0x43, (byte) 0x10, (byte) 0x3E,
            (byte) 0x1A, (byte) 0x21, (byte) 0x00, (byte) 0x0,
            (byte) 0x0, (byte) 0x00,(byte) 0x14, (byte) 0xF7
        };

        SysexMessage msg = MidiTestUtils.createSysexMessage(faderMsg);

        server.addtoinputqueue(msg);
        server.processIncomingMidiForTest();

        assertFalse(broadcasts.isEmpty(), "Expected at least one broadcast message");
    }

    @Test
    void testHeartbeat() throws Exception {

        List<String> broadcasts = new ArrayList<>();

        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");
        SysexParser parser = new SysexParser(mappings);
        CanonicalRegistry registry = new CanonicalRegistry(mappings, parser);

        MidiServer server = new MidiServer(registry);

        byte[] heartbeat = {
            (byte) 0xF0, (byte) 0x43, (byte) 0x10, (byte) 0x30,
            (byte) 0x7F, (byte) 0x7F, (byte) 0xF7
        };

        SysexMessage msg = MidiTestUtils.createSysexMessage(heartbeat);

        server.addtoinputqueue(msg);
        assertDoesNotThrow(() ->         server.processIncomingMidiForTest());
    }
}