package MidiControl.functional.GuiToHardware;

import MidiControl.ControlServer.GuiInputHandler;
import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.ControlInstance;
import MidiControl.Controls.SubControl;
import MidiControl.MidiDeviceManager.TransportMode;
import MidiControl.Mocks.MockMidiOut;
import MidiControl.Mocks.MockMidiServer;
import MidiControl.NrpnUtils.NrpnMapping;
import MidiControl.Routing.OutputRouter;
import MidiControl.Server.MidiServer;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

public class GuiToHardwareChangeTest {

    @Test
    public void testGuiChangeSendsCorrectSysex() {

        // --- canonical setup ---
        List<SysexMapping> mappings =
                SysexMappingLoader.loadMappingsFromResource("MidiControl/m7cl_sysex_mappings.json");
        SysexParser parser = new SysexParser(mappings);
        CanonicalRegistry registry = new CanonicalRegistry(mappings, parser);
        MidiServer server = new MockMidiServer(registry);
        MockMidiOut out = new MockMidiOut();
        server.getMidiDeviceManager().setMidiOutForTest(out);
        OutputRouter router = new OutputRouter(registry,server.getMidiDeviceManager());
        GuiInputHandler gui = new GuiInputHandler(router);

        ControlInstance ci =
            registry.getGroups().values().stream()
                .flatMap((ControlGroup group) ->
                    group.getSubcontrols().values().stream())
                .flatMap((SubControl sub) ->
                    sub.getInstances().stream())
                .filter(inst -> inst.getNrpn().isEmpty())
                .findFirst()
                .orElseThrow();

        int newValue = 55;

        // --- act ---
        gui.handleGuiChange(ci.getCanonicalId(), newValue);

        // --- assert ---
        byte[] sent = out.getSentMessages().get(out.getSentMessages().size()-1);
        byte[] expected = ci.getSysex().buildChangeMessage(newValue, ci.getIndex());

        assertArrayEquals(expected, sent);
    }

    @Test
    public void testGuiChangeSendsCorrectNrpn() {

        // --- canonical setup ---
        List<SysexMapping> mappings =
                SysexMappingLoader.loadMappingsFromResource("MidiControl/m7cl_sysex_mappings.json");
        SysexParser parser = new SysexParser(mappings);
        CanonicalRegistry registry = new CanonicalRegistry(mappings, parser);
        MidiServer server = new MockMidiServer(registry);
        MockMidiOut out = new MockMidiOut();
        server.getMidiDeviceManager().setMidiOutForTest(out);

        server.getMidiDeviceManager().setTransportMode(TransportMode.NRPN);

        OutputRouter router = new OutputRouter(registry,server.getMidiDeviceManager());;
        GuiInputHandler gui = new GuiInputHandler(router);

        // Pick ANY real control instance
        ControlInstance ci =
            registry.getGroups().values().stream()
                .flatMap((ControlGroup group) -> group.getSubcontrols().values().stream())
                .flatMap((SubControl sub) -> sub.getInstances().stream())
                .findFirst()
                .orElseThrow();

        // Create a synthetic NRPN mapping for this canonical ID
        NrpnMapping nrpn = new NrpnMapping(
            "12",   // arbitrary MSB
            "34",   // arbitrary LSB
            ci.getCanonicalId()
        );

        // Attach NRPN to this control
        ci.setNrpn(nrpn);

        int newValue = 55;

        // --- act ---
        gui.handleGuiChange(ci.getCanonicalId(), newValue);

        // --- assert ---
        List<byte[]> sent = out.getSentMessages();
        List<byte[]> expected = nrpn.buildNrpnBytes(Optional.empty(), newValue);

        assertEquals(expected.size(), sent.size());
        for (int i = 0; i < expected.size(); i++) {
            assertArrayEquals(expected.get(i), sent.get(i));
        }
    }
}