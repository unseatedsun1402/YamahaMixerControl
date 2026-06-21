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
import MidiControl.Routing.HardwareOutputRouter;
import MidiControl.Server.MidiServer;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class GuiToHardwareChangeTest {
    @Test
    public void testGuiChangeSendsCorrectSysex() {
        List<SysexMapping> mappings =
                SysexMappingLoader.loadMappingsFromResource("MidiControl/m7cl_sysex_mappings.json");

        SysexParser parser = new SysexParser(mappings);
        CanonicalRegistry registry = new CanonicalRegistry(mappings, parser);

        MidiServer server = new MockMidiServer(registry);
        MockMidiOut out = new MockMidiOut();

        server.getMidiDeviceManager().setMidiOutForTest(out);
        server.getMidiDeviceManager().setTransportMode(TransportMode.SYSEX);

        HardwareOutputRouter router =
            new HardwareOutputRouter(registry, server.getMidiDeviceManager());

        GuiInputHandler gui = new GuiInputHandler(router);

        ControlInstance ci = registry.resolve("kInputFader.kFader.1");

        int newValue = 55;

        gui.handleGuiChange(ci.getCanonicalId(), newValue);

        assertEquals(1, out.getSentMessages().size());

        byte[] sent = out.getSentMessages().get(0);
        byte[] expected = ci.getSysex().buildChangeMessage(newValue, ci.getIndex());

        assertArrayEquals(expected, sent);
    }

    @Test
    public void testGuiChangeSendsCorrectNrpn() {
        List<SysexMapping> mappings =
                SysexMappingLoader.loadMappingsFromResource("MidiControl/m7cl_sysex_mappings.json");

        SysexParser parser = new SysexParser(mappings);
        CanonicalRegistry registry = new CanonicalRegistry(mappings, parser);

        MidiServer server = new MockMidiServer(registry);
        MockMidiOut out = new MockMidiOut();

        server.getMidiDeviceManager().setMidiOutForTest(out);
        server.getMidiDeviceManager().setTransportMode(TransportMode.NRPN);

        HardwareOutputRouter router =
            new HardwareOutputRouter(registry, server.getMidiDeviceManager());

        GuiInputHandler gui = new GuiInputHandler(router);

        ControlInstance ci =
            registry.getGroups().values().stream()
                .flatMap((ControlGroup group) -> group.getSubcontrols().values().stream())
                .flatMap((SubControl sub) -> sub.getInstances().stream())
                .findFirst()
                .orElseThrow();

        NrpnMapping nrpn = new NrpnMapping(
            "12",
            "34",
            ci.getCanonicalId(),
            "CC6_ONLY"
        );

        ci.setNrpn(nrpn);

        int canonicalValue = 55;

        int expectedTransportValue =
            ci.toNrpnValue(canonicalValue, nrpn.getMin(), nrpn.getMax());

        gui.handleGuiChange(ci.getCanonicalId(), canonicalValue);

        List<byte[]> sent = out.getSentMessages();
        List<byte[]> expected = nrpn.buildNrpnBytes(expectedTransportValue);

        assertEquals(expected.size(), sent.size());

        for (int i = 0; i < expected.size(); i++) {
            assertArrayEquals(expected.get(i), sent.get(i));
        }
    }
}