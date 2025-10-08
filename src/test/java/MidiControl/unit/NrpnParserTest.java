package MidiControl.unit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import javax.sound.midi.ShortMessage;


import MidiControl.TestUtilities.MidiTestUtils;
import MidiControl.TestUtilities.NrpnMocker;
import MidiControl.Utilities.NrpnMapping;
import MidiControl.Utilities.NrpnParser;
import MidiControl.Utilities.NrpnRegistry;

@Tag("unit")
public class NrpnParserTest {
    @Test
    public void testMix9Send3Resolution() throws Exception {
    NrpnParser parser = new NrpnParser();
    NrpnMocker mocker = new NrpnMocker(parser);
    NrpnRegistry.getInstance().loadFromClasspath("MidiControl/nrpn_mappings.json");

    mocker.sendNrpn(0x01, 0x00, 8192);
    assertTrue(NrpnRegistry.resolve(0x01, 0x00).isPresent());
    }

    @Test
    void testValidNrpnWithOnlyValueMsb() throws Exception {
    NrpnRegistry.getInstance().buildProfileMapping();
    NrpnParser parser = new NrpnParser();
    TestDispatchTarget target = new TestDispatchTarget();
    NrpnParser.setDispatchTarget(target);

    MidiTestUtils.sendMockNrpn(parser, 1, 0, 8192); // Only MSB used

    assertNotNull(target.lastMapping, "Mapping should be dispatched");
    assertEquals("Mix 9 send 3 control", target.lastMapping.label());
    assertEquals(8192, target.lastValue, "Parsed value should match expected");
    }


    @Test
    void testValidNrpnWithFull14BitValue() throws Exception {
    NrpnParser parser = new NrpnParser();
    // Value = 12345 = 0x3039 → MSB = 96, LSB = 57
    parser.parse(MidiTestUtils.createControlChange(0, 99, 1));  // MSB address
    parser.parse(MidiTestUtils.createControlChange(0, 98, 0));  // LSB address
    parser.parse(MidiTestUtils.createControlChange(0, 6, 96));  // Value MSB
    parser.parse(MidiTestUtils.createControlChange(0, 38, 57)); // Value LSB

    Optional<NrpnMapping> resolved = NrpnRegistry.resolve(1, 0);
    assertTrue(resolved.isPresent());
    assertEquals("Mix 9 send 3 control", resolved.get().label());
    }


    @Test
    void testParserIgnoresIncompleteSequence() throws Exception {
        NrpnParser parser = new NrpnParser();
        ShortMessage msg = MidiTestUtils.createControlChange(0, 99, 1); // Only MSB
        parser.parse(msg);

        // Should not dispatch or resolve
        Optional<NrpnMapping> resolved = NrpnRegistry.resolve(1, 0);
        assertTrue(resolved.isPresent(), "Registry should still contain mapping");
    }

    @Test
    void testParserRejectsInvalidControlChange() throws Exception {
    NrpnParser parser = new NrpnParser();
    ShortMessage msg = new ShortMessage(ShortMessage.NOTE_ON, 0, 60, 127); // Not a control change

    assertDoesNotThrow(() -> parser.parse(msg), "Parser should ignore non-control messages");
    }
}
