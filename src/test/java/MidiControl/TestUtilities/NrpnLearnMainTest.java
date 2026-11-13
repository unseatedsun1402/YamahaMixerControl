package MidiControl.TestUtilities;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NrpnLearnMainTest {
    @Test
    void loadsTableAndKeepsRepeatedLabelsDistinct() throws Exception {
        List<NrpnLearnMain.Reference> table = NrpnLearnMain.loadTable(Path.of("m7cl_nrpn.csv"));
        assertEquals(361, table.size());
        List<NrpnLearnMain.Reference> mix9 = table.stream()
                .filter(row -> row.label.equals("MIX9 SEND")).toList();
        assertTrue(mix9.size() > 1);
        assertEquals(0x7e, mix9.get(0).tableStart);
        assertNotEquals(mix9.get(0).tableStart, mix9.get(1).tableStart);
    }

    @Test
    void capturesLsbFirstAndKeepsAddressesAndChannelsSeparate() {
        List<NrpnLearnMain.Cc> messages = List.of(
                cc(0, 98, 0x16), cc(0, 99, 0x41), cc(0, 6, 0), cc(0, 38, 0),
                cc(0, 6, 127), cc(0, 38, 127),
                cc(0, 98, 0x17), cc(0, 99, 0x41), cc(0, 6, 64), cc(0, 38, 64),
                cc(1, 99, 0x41), cc(1, 98, 0x16), cc(1, 6, 7));
        List<NrpnLearnMain.Observation> found = NrpnLearnMain.analyse(messages);
        assertEquals(3, found.size());
        var first = found.get(0);
        assertEquals(1, first.midiChannel);
        assertEquals(0x2096, first.address);
        assertEquals(0, first.minCc6);
        assertEquals(127, first.maxCc6);
        assertEquals(0, first.minCc38);
        assertEquals(127, first.maxCc38);
        assertEquals(0, first.min14);
        assertEquals(16383, first.max14);
        assertEquals(0x2097, found.get(1).address);
        assertEquals(2, found.get(2).midiChannel);
        assertEquals(7, found.get(2).minCc6);
        assertNull(found.get(2).min14);
    }

    @Test
    void preservesStandaloneDataAndIgnoresRpn() {
        List<NrpnLearnMain.Observation> found = NrpnLearnMain.analyse(List.of(
                cc(0, 99, 1), cc(0, 98, 2), cc(0, 6, 4), cc(0, 6, 8),
                cc(0, 38, 3), cc(0, 38, 9),
                cc(0, 101, 0), cc(0, 100, 0), cc(0, 6, 100),
                cc(0, 98, 3), cc(0, 99, 1), cc(0, 38, 12)));
        assertEquals(2, found.size());
        var first = found.get(0);
        assertEquals(4, first.minCc6);
        assertEquals(8, first.maxCc6);
        assertEquals(3, first.minCc38);
        assertEquals(9, first.maxCc38);
        assertEquals((8 << 7) | 3, first.min14);
        assertEquals(first.min14, first.max14);
        assertNull(found.get(1).minCc6);
        assertEquals(12, found.get(1).minCc38);
        assertNull(found.get(1).min14);
    }

    @Test
    void probeRequiresExplicitConfirmationAndRecordsOnlyTestedAddress() throws Exception {
        NrpnLearnMain.Reference reference = new NrpnLearnMain.Reference("PAN", 0x2096, 0x20cd);
        RecordingReceiver sender = new RecordingReceiver();
        try (Scanner cancelled = new Scanner("1\n1\n127\n-1\nno\n")) {
            NrpnLearnMain.verify(cancelled, new NrpnLearnMain.CaptureReceiver(), sender, reference);
        }
        assertTrue(sender.messages.isEmpty());
        assertTrue(reference.tests.isEmpty());

        try (Scanner confirmed = new Scanner("2\n3\n64\n1\nSEND\nn\nn\n")) {
            NrpnLearnMain.verify(confirmed, new NrpnLearnMain.CaptureReceiver(), sender, reference);
        }
        assertEquals(List.of("2:99:65", "2:98:23", "2:6:64", "2:38:1"), sender.messages);
        assertEquals(1, reference.tests.size());
        sender.messages.clear();
        try (Scanner cc6Only = new Scanner("1\n1\n127\n-1\nSEND\ny\n")) {
            NrpnLearnMain.verify(cc6Only, new NrpnLearnMain.CaptureReceiver(), sender, reference);
        }
        assertEquals(List.of("0:99:65", "0:98:22", "0:6:127"), sender.messages);
    }

    @Test
    void addsOnlyEvidenceAndResumesExistingReport(@TempDir Path temp) throws Exception {
        var ref = new NrpnLearnMain.Reference("PAN", 0x2096, 0x20cd);
        var observed = NrpnLearnMain.analyse(List.of(
                cc(0, 99, 65), cc(0, 98, 22), cc(0, 6, 0),
                cc(0, 98, 53), cc(0, 6, 127)));
        Path path = temp.resolve("report.json");
        JsonObject report = NrpnLearnMain.loadReport(path, "01V96i");
        assertFalse(NrpnLearnMain.addEvidence(report, ref));
        assertEquals(0, report.getAsJsonArray("controls").size());
        ref.learned.add(new NrpnLearnMain.Learning(observed));
        assertTrue(NrpnLearnMain.addEvidence(report, ref));
        NrpnLearnMain.saveReport(path, report, true);
        String first = Files.readString(path);
        assertThrows(java.nio.file.FileAlreadyExistsException.class,
                () -> NrpnLearnMain.saveReport(path, report, true));
        assertTrue(first.contains("\"highestObservedAddress\": 8373"));
        assertTrue(first.contains("\"observedAddressCount\": 2"));
        assertTrue(first.contains("\"reference_candidate\""));
        assertFalse(first.contains("canonical"));

        JsonObject resumed = NrpnLearnMain.loadReport(path, "01v96i");
        var next = new NrpnLearnMain.Reference("PAN", 0x2096, 0x20cd);
        next.learned.add(new NrpnLearnMain.Learning(observed));
        try (Scanner probe = new Scanner("1\n1\n127\n-1\nSEND\nn\nn\n")) {
            NrpnLearnMain.verify(probe, new NrpnLearnMain.CaptureReceiver(), new RecordingReceiver(), next);
        }
        assertTrue(NrpnLearnMain.addEvidence(resumed, next));
        var another = new NrpnLearnMain.Reference("PAN", 0x2100, 0x2101);
        another.learned.add(new NrpnLearnMain.Learning(observed));
        assertTrue(NrpnLearnMain.addEvidence(resumed, another));
        NrpnLearnMain.saveReport(path, resumed, false);
        JsonObject saved = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        assertEquals(2, saved.getAsJsonArray("controls").size());
        assertEquals(2, saved.getAsJsonArray("controls").get(0).getAsJsonObject()
                .getAsJsonArray("learned").size());
        assertEquals("not_confirmed", saved.getAsJsonArray("controls").get(0).getAsJsonObject()
                .getAsJsonArray("tests").get(0).getAsJsonObject().get("status").getAsString());
        assertThrows(java.io.IOException.class, () -> NrpnLearnMain.loadReport(path, "M7CL"));
    }

    @Test
    void removesUntestedLegacyEntriesButPreservesEvidenceAndExtraFields(@TempDir Path temp)
            throws Exception {
        Path path = temp.resolve("legacy.json");
        JsonObject legacy = NrpnLearnMain.loadReport(path, "01v96i");
        JsonObject untouched = JsonParser.parseString("""
                {"label":"unused","tableStart":0,"tableEnd":1,"tests":[],"learned":[]}
                """).getAsJsonObject();
        JsonObject captured = JsonParser.parseString("""
                {"label":"PAN","tableStart":8342,"tableEnd":8397,"tests":[],
                 "learned":[{"status":"observed","extra":"keep me"}],"note":"keep this too"}
                """).getAsJsonObject();
        legacy.addProperty("extraMetadata", "preserve");
        legacy.getAsJsonArray("controls").add(untouched);
        legacy.getAsJsonArray("controls").add(captured);
        NrpnLearnMain.saveReport(path, legacy, true);

        JsonObject loaded = NrpnLearnMain.loadReport(path, "01v96i");
        assertEquals("preserve", loaded.get("extraMetadata").getAsString());
        assertEquals(1, loaded.getAsJsonArray("controls").size());
        assertEquals(captured, loaded.getAsJsonArray("controls").get(0));
        NrpnLearnMain.saveReport(path, loaded, false);
        assertEquals(loaded, JsonParser.parseString(Files.readString(path)));
    }

    private static NrpnLearnMain.Cc cc(int channel, int controller, int value) {
        return new NrpnLearnMain.Cc(channel, controller, value);
    }

    private static class RecordingReceiver implements Receiver {
        final List<String> messages = new ArrayList<>();

        @Override public void send(MidiMessage message, long timestamp) {
            ShortMessage cc = (ShortMessage) message;
            messages.add(cc.getChannel() + ":" + cc.getData1() + ":" + cc.getData2());
        }

        @Override public void close() { }
    }
}
