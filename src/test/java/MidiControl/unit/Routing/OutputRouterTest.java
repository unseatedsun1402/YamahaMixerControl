package MidiControl.unit.Routing;

import MidiControl.Routing.OutputRouter;
import MidiControl.Controls.*;
import MidiControl.MidiDeviceManager.*;
import MidiControl.Server.MidiServer;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.NrpnUtils.NrpnMapping;

import org.junit.jupiter.api.Test;

import java.util.*;

import javax.sound.midi.MidiDevice;

import static org.junit.jupiter.api.Assertions.*;

public class OutputRouterTest {

    // ------------------------------------------------------------
    // Mock MIDI OUT
    // ------------------------------------------------------------
    private static class MockMidiOut implements MidiOutput {
        public final List<byte[]> sent = new ArrayList<>();

        @Override
        public void sendMessage(byte[] data) {
            sent.add(data);
        }

        @Override
        public void sendMessage(javax.sound.midi.MidiMessage message) {
            sent.add(message.getMessage());
        }

        @Override
        public MidiDevice.Info getDeviceInfo() { return null; }

        @Override
        public boolean isOpen() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'isOpen'");
        }

        @Override
        public void close() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'close'");
        }
    }

    // ------------------------------------------------------------
    // Mock IO Manager
    // ------------------------------------------------------------
    private static class MockIOManager extends MidiIOManager {
        private final TransportMode mode;
        private final MockMidiOut out;

        public MockIOManager(TransportMode mode, MockMidiOut out) {
            super(null);
            this.mode = mode;
            this.out = out;
        }

        @Override
        public TransportMode getTransportMode() {
            return mode;
        }

        @Override
        public MidiOutput getMidiOut() {
            return out;
        }

        @Override
        public void sendAsync(byte[] message){
            this.out.sent.add(message);
        }
    }

    // ------------------------------------------------------------
    // Mock Registry
    // ------------------------------------------------------------
    private static class MockRegistry extends CanonicalRegistry {
        private final Map<String, ControlInstance> map = new HashMap<>();

        public MockRegistry() {
            super(List.of(), null);
        }

        public void add(String id, ControlInstance ci) {
            map.put(id, ci);
        }

        @Override
        public ControlInstance resolveCanonicalId(String id) {
            return map.get(id);
        }
    }

    // ------------------------------------------------------------
    // Mock Server
    // ------------------------------------------------------------
    private static class MockServer extends MidiServer {
        private final MockRegistry registry;
        private final MidiIOManager io;

        public MockServer(MockRegistry registry, MidiIOManager io) {
            super(null, null);
            this.registry = registry;
            this.io = io;
        }

        @Override
        public CanonicalRegistry getCanonicalRegistry() {
            return registry;
        }

        @Override
        public MidiIOManager getMidiDeviceManager() {
            return io;
        }
    }

    // ------------------------------------------------------------
    // Helpers to build fake mappings
    // ------------------------------------------------------------
    private SysexMapping fakeSysex() {
        return new SysexMapping(
                "g", 0, 1, "s", 0, 0L,
                0, 0, 127, 0,
                "test",
                List.of("F0", "01"), // change
                List.of("F0", "02"),  // request
                0
        ) {
            @Override
            public byte[] buildChangeMessage(int value, int index) {
                return new byte[]{(byte) 0xF0, 0x10, (byte) value, (byte) 0xF7};
            }

            @Override
            public byte[] buildRequestMessage(int index) {
                return new byte[]{(byte) 0xF0, 0x20, (byte) index, (byte) 0xF7};
            }
        };
    }

    private NrpnMapping fakeNrpn() {
        return new NrpnMapping("0", "0", "test.canonical.1") {
            @Override
            public List<byte[]> buildNrpnBytes(Optional<ControlInstance> ci, int value) {
                return List.of(
                        new byte[]{(byte) 0xB0, 0x63, 0x01},
                        new byte[]{(byte) 0xB0, 0x62, 0x02},
                        new byte[]{(byte) 0xB0, 0x06, (byte) value}
                );
            }
        };
    }

    private ControlInstance fakeInstance(boolean withNrpn) {
        ControlGroup g = new ControlGroup("g");
        SubControl sc = new SubControl(g, "s");
        ControlInstance ci = new ControlInstance(sc, 0, fakeSysex(), null);

        if (withNrpn) {
            ci.setNrpn(fakeNrpn());
        }

        return ci;
    }

    // ------------------------------------------------------------
    // TESTS
    // ------------------------------------------------------------

    @Test
    public void testApplyChange_SysexMode() {
        MockMidiOut out = new MockMidiOut();
        MockIOManager io = new MockIOManager(TransportMode.SYSEX, out);
        MockRegistry reg = new MockRegistry();

        io.setMidiOutForTest(out);

        ControlInstance ci = fakeInstance(false);
        reg.add("fader.1", ci);

        OutputRouter router = new OutputRouter(reg,io);

        router.applyChange("fader.1", 55);

        assertEquals(1, out.sent.size());
        assertArrayEquals(new byte[]{(byte) 0xF0, 0x10, 55, (byte) 0xF7}, out.sent.get(0));
    }

    @Test
    public void testApplyChange_NrpnMode_WithNrpn() {
        MockMidiOut out = new MockMidiOut();
        MockIOManager io = new MockIOManager(TransportMode.NRPN, out);
        MockRegistry reg = new MockRegistry();

        ControlInstance ci = fakeInstance(true);
        reg.add("pan.2", ci);

        OutputRouter router = new OutputRouter(reg,io);

        router.applyChange("pan.2", 99);

        assertEquals(3, out.sent.size());
        assertArrayEquals(new byte[]{(byte) 0xB0, 0x63, 0x01}, out.sent.get(0));
        assertArrayEquals(new byte[]{(byte) 0xB0, 0x62, 0x02}, out.sent.get(1));
        assertArrayEquals(new byte[]{(byte) 0xB0, 0x06, 99}, out.sent.get(2));
    }

    @Test
    public void testApplyChange_NrpnMode_NoNrpn_FallbackToSysex() {
        MockMidiOut out = new MockMidiOut();
        MockIOManager io = new MockIOManager(TransportMode.NRPN, out);
        MockRegistry reg = new MockRegistry();

        ControlInstance ci = fakeInstance(false);
        reg.add("gain.3", ci);

        OutputRouter router = new OutputRouter(reg,io);

        router.applyChange("gain.3", 12);

        assertEquals(1, out.sent.size());
        assertArrayEquals(new byte[]{(byte) 0xF0, 0x10, 12, (byte) 0xF7}, out.sent.get(0));
    }

    @Test
    public void testApplyRequest() {
        MockMidiOut out = new MockMidiOut();
        MockIOManager io = new MockIOManager(TransportMode.SYSEX, out);
        MockRegistry reg = new MockRegistry();

        ControlInstance ci = fakeInstance(false);
        reg.add("fader.1", ci);

        OutputRouter router = new OutputRouter(reg,io);

        router.applyRequest("fader.1");

        assertEquals(1, out.sent.size());
        assertArrayEquals(new byte[]{(byte) 0xF0, 0x20, 0, (byte) 0xF7}, out.sent.get(0));
    }

    @Test
    public void testApplyChange_UnknownId_NoSend() {
        MockMidiOut out = new MockMidiOut();
        MockIOManager io = new MockIOManager(TransportMode.SYSEX, out);
        MockRegistry reg = new MockRegistry();

        OutputRouter router = new OutputRouter(reg,io);

        router.applyChange("does.not.exist", 10);

        assertTrue(out.sent.isEmpty());
    }

    @Test
    public void testApplyRequest_UnknownId_Throws() {
        MockMidiOut out = new MockMidiOut();
        MockIOManager io = new MockIOManager(TransportMode.SYSEX, out);
        MockRegistry reg = new MockRegistry();

        OutputRouter router = new OutputRouter(reg,io);

        assertThrows(IllegalArgumentException.class, () ->
                router.applyRequest("does.not.exist")
        );
    }
}