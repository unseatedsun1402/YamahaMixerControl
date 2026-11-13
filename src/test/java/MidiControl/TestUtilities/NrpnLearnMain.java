package MidiControl.TestUtilities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;

public class NrpnLearnMain {
    private static final Path TABLE = Path.of("m7cl_nrpn.csv");
    private static final Pattern TABLE_ROW = Pattern.compile(
            "^\"([^\"]+)\",\"([0-9A-Fa-f]{4})\",\"([0-9A-Fa-f]{4})\"$");
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String REFERENCE_SOURCE =
            "M7CL owner's manual pages 274-276; m7cl_nrpn.csv transcription";

    public static void main(String[] args) throws Exception {
        List<Reference> references = loadTable(TABLE);
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Reference: M7CL owner's manual, pages 274-276.");
            System.out.println("Other desks must be verified; the table does not specify test values.");
            System.out.println("Probes change desk settings and are not automatically restored.");
            System.out.print("Desk model (for report): ");
            String desk = scanner.nextLine().trim();
            if (desk.isEmpty()) throw new IllegalArgumentException("Desk model is required");

            Path reportPath = chooseReportPath(scanner);
            boolean newReport = !Files.exists(reportPath);
            JsonObject report = loadReport(reportPath, desk);
            saveReport(reportPath, report, newReport);

            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
            int inputIndex = chooseDevice(scanner, infos, false);
            MidiDevice input = MidiSystem.getMidiDevice(infos[inputIndex]);
            MidiDevice output = null;
            Transmitter transmitter = null;
            Receiver sender = null;
            CaptureReceiver capture = new CaptureReceiver();
            try {
                input.open();
                transmitter = input.getTransmitter();
                transmitter.setReceiver(capture);
                System.out.print("Enable MIDI output for verification? [y/N]: ");
                if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
                    int outputIndex = chooseDevice(scanner, infos, true);
                    output = outputIndex == inputIndex ? input : MidiSystem.getMidiDevice(infos[outputIndex]);
                    if (!output.isOpen()) output.open();
                    sender = output.getReceiver();
                }

                while (true) {
                    System.out.print("\nChoose [l]earn, [v]erify, or [q]uit: ");
                    String mode = scanner.nextLine().trim().toLowerCase();
                    if (mode.equals("q")) break;
                    if (!mode.equals("l") && !mode.equals("v")) {
                        System.out.println("Choose l, v, or q.");
                        continue;
                    }
                    if (mode.equals("v") && sender == null) {
                        System.out.println("Output is not enabled. Restart to enable it.");
                        continue;
                    }
                    int selected = chooseReference(scanner, references);
                    if (selected < 0) continue;
                    Reference reference = references.get(selected);
                    if (mode.equals("l")) {
                        learn(scanner, capture, reference);
                    } else {
                        verify(scanner, capture, sender, reference);
                    }
                    if (addEvidence(report, reference)) {
                        saveReport(reportPath, report, false);
                        reference.tests.clear();
                        reference.learned.clear();
                        System.out.println("Report saved: " + reportPath);
                    }
                }
            } finally {
                capture.close();
                if (transmitter != null) transmitter.close();
                if (sender != null) sender.close();
                if (output != null && output != input) output.close();
                input.close();
            }
        }
    }

    static List<Reference> loadTable(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.get(0).equals("\"Parameter Name\",\"From HEX\",\"To HEX\"")) {
            throw new IOException("Unexpected NRPN table header: " + path);
        }
        List<Reference> references = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            Matcher row = TABLE_ROW.matcher(lines.get(i));
            if (!row.matches()) throw new IOException("Invalid NRPN table row " + (i + 1));
            int start = Integer.parseInt(row.group(2), 16);
            int end = Integer.parseInt(row.group(3), 16);
            if (start > end || end > 0x3FFF) {
                throw new IOException("Invalid NRPN range at row " + (i + 1));
            }
            references.add(new Reference(row.group(1), start, end));
        }
        if (references.isEmpty()) throw new IOException("NRPN reference table is empty");
        return references;
    }

    private static Path chooseReportPath(Scanner scanner) {
        System.out.print("JSON report path to create or continue [nrpn_learn_report.json]: ");
        String value = scanner.nextLine().trim();
        return Path.of(value.isEmpty() ? "nrpn_learn_report.json" : value);
    }

    private static int chooseDevice(Scanner scanner, MidiDevice.Info[] infos, boolean output)
            throws Exception {
        System.out.println(output ? "MIDI outputs:" : "MIDI inputs:");
        List<Integer> available = new ArrayList<>();
        for (int i = 0; i < infos.length; i++) {
            MidiDevice device = MidiSystem.getMidiDevice(infos[i]);
            if (output ? device.getMaxReceivers() != 0 : device.getMaxTransmitters() != 0) {
                available.add(i);
                System.out.printf("[%d] %s | %s%n", i, infos[i].getName(), infos[i].getDescription());
            }
        }
        if (available.isEmpty()) throw new IllegalStateException("No suitable MIDI devices available");
        while (true) {
            int index = askInt(scanner, "Device index:", 0, infos.length - 1);
            if (available.contains(index)) return index;
            System.out.println("Device does not support the requested direction.");
        }
    }

    private static int chooseReference(Scanner scanner, List<Reference> references) {
        System.out.print("Filter table labels (blank shows all): ");
        String filter = scanner.nextLine().trim().toLowerCase();
        List<Integer> matches = new ArrayList<>();
        for (int i = 0; i < references.size(); i++) {
            Reference reference = references.get(i);
            if (reference.label.toLowerCase().contains(filter)) {
                matches.add(i);
                System.out.printf("[%d] %s  %04X-%04X%n",
                        i + 1, reference.label, reference.tableStart, reference.tableEnd);
            }
        }
        if (matches.isEmpty()) {
            System.out.println("No matching table entries.");
            return -1;
        }
        int selected = askInt(scanner, "Table entry number (0 cancels):", 0, references.size());
        if (selected == 0) return -1;
        if (!matches.contains(selected - 1)) {
            System.out.println("Selection is not in the filtered list.");
            return -1;
        }
        return selected - 1;
    }

    static void verify(Scanner scanner, CaptureReceiver capture, Receiver sender,
            Reference reference) throws Exception {
        int instance = askInt(scanner, "Table instance number (1-based):", 1,
                reference.tableEnd - reference.tableStart + 1);
        int channel = askInt(scanner, "MIDI channel (1-16):", 1, 16) - 1;
        int dataMsb = askInt(scanner, "Data entry CC6 (0-127):", 0, 127);
        int lsbInput = askInt(scanner, "Data entry CC38 (0-127, or -1 to omit):", -1, 127);
        Integer dataLsb = lsbInput < 0 ? null : lsbInput;
        int address = reference.tableStart + instance - 1;
        System.out.printf("Will send %s instance %d, address %02X:%02X, channel %d, CC6=%d, CC38=%s.%n",
                reference.label, instance, address >> 7, address & 0x7F, channel + 1,
                dataMsb, dataLsb == null ? "omitted" : dataLsb);
        System.out.print("This may change an unexpected desk control. Type SEND to proceed: ");
        if (!scanner.nextLine().trim().equals("SEND")) {
            System.out.println("Probe cancelled.");
            return;
        }
        sendNrpn(sender, channel, address, dataMsb, dataLsb);
        String answer;
        do {
            System.out.print("Did the intended control respond? [y/n]: ");
            answer = scanner.nextLine().trim().toLowerCase();
        } while (!answer.equals("y") && !answer.equals("n"));
        reference.tests.add(new Verification(instance, channel + 1, address, dataMsb, dataLsb,
                answer.equals("y") ? "confirmed_by_user" : "not_confirmed"));
        if (answer.equals("n")) {
            System.out.print("Learn this control now? [y/N]: ");
            if (scanner.nextLine().trim().equalsIgnoreCase("y")) learn(scanner, capture, reference);
        }
    }

    static void sendNrpn(Receiver sender, int channel, int address, int dataMsb, Integer dataLsb)
            throws Exception {
        int[][] controls = dataLsb == null
                ? new int[][] {{99, address >> 7}, {98, address & 0x7F}, {6, dataMsb}}
                : new int[][] {{99, address >> 7}, {98, address & 0x7F}, {6, dataMsb}, {38, dataLsb}};
        for (int[] control : controls) {
            ShortMessage message = new ShortMessage(
                    ShortMessage.CONTROL_CHANGE, channel, control[0], control[1]);
            sender.send(message, -1);
            Thread.sleep(4);
        }
    }

    private static void learn(Scanner scanner, CaptureReceiver capture, Reference reference) {
        System.out.println("Move the selected control across its range, then its other instances.");
        System.out.println("Only manipulate this control during capture; press ENTER to finish.");
        capture.start();
        scanner.nextLine();
        List<Observation> observations = analyse(capture.stop());
        if (observations.isEmpty()) {
            System.out.println("No complete NRPN data-entry messages captured; nothing learned.");
            return;
        }
        observations.forEach(observation -> System.out.println(observation.summary()));
        reference.learned.add(new Learning(observations));
    }

    static List<Observation> analyse(List<Cc> messages) {
        Map<Integer, ChannelState> channels = new HashMap<>();
        Map<Integer, Observation> observations = new TreeMap<>();
        for (Cc cc : messages) {
            ChannelState state = channels.computeIfAbsent(cc.channel, ignored -> new ChannelState());
            switch (cc.controller) {
                case 99 -> {
                    flush(state, observations, cc.channel);
                    state.msb = cc.value;
                    state.nrpn = true;
                }
                case 98 -> {
                    flush(state, observations, cc.channel);
                    state.lsb = cc.value;
                    state.nrpn = true;
                }
                case 101, 100 -> {
                    flush(state, observations, cc.channel);
                    state.nrpn = false;
                    state.msb = -1;
                    state.lsb = -1;
                }
                case 6 -> {
                    flush(state, observations, cc.channel);
                    if (state.nrpn && state.msb >= 0 && state.lsb >= 0) {
                        state.pendingAddress = (state.msb << 7) | state.lsb;
                        state.pendingMsb = cc.value;
                    }
                }
                case 38 -> {
                    if (state.pendingMsb >= 0) {
                        state.pendingLsb = cc.value;
                        flush(state, observations, cc.channel);
                    } else if (state.nrpn && state.msb >= 0 && state.lsb >= 0) {
                        int address = (state.msb << 7) | state.lsb;
                        int key = (cc.channel << 14) | address;
                        observations.computeIfAbsent(key,
                                ignored -> new Observation(cc.channel + 1, address))
                                .addLsb(cc.value);
                    }
                }
                default -> { }
            }
        }
        channels.forEach((channel, state) -> flush(state, observations, channel));
        return new ArrayList<>(observations.values());
    }

    private static void flush(ChannelState state, Map<Integer, Observation> observations, int channel) {
        if (state.pendingMsb < 0) return;
        int key = (channel << 14) | state.pendingAddress;
        Observation observation = observations.computeIfAbsent(key,
                ignored -> new Observation(channel + 1, state.pendingAddress));
        observation.add(state.pendingMsb, state.pendingLsb);
        state.pendingMsb = -1;
        state.pendingLsb = -1;
    }

    private static int askInt(Scanner scanner, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt + " ");
            try {
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value >= min && value <= max) return value;
            } catch (NumberFormatException ignored) {
                // Retry invalid console input.
            }
            System.out.printf("Enter an integer from %d to %d.%n", min, max);
        }
    }

    static JsonObject loadReport(Path path, String desk) throws IOException {
        if (!Files.exists(path)) {
            JsonObject report = new JsonObject();
            report.addProperty("deskModel", desk);
            report.addProperty("referenceSource", REFERENCE_SOURCE);
            report.add("controls", new JsonArray());
            return report;
        }
        try {
            JsonObject report = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            if (!desk.equalsIgnoreCase(report.get("deskModel").getAsString())
                    || !REFERENCE_SOURCE.equals(report.get("referenceSource").getAsString())) {
                throw new IOException("Report desk or reference does not match: " + path);
            }
            JsonArray retained = new JsonArray();
            for (JsonElement element : report.getAsJsonArray("controls")) {
                JsonObject control = element.getAsJsonObject();
                control.get("label").getAsString();
                control.get("tableStart").getAsInt();
                control.get("tableEnd").getAsInt();
                JsonArray tests = control.getAsJsonArray("tests");
                JsonArray learned = control.getAsJsonArray("learned");
                if (tests == null || learned == null) {
                    throw new IOException("Missing evidence arrays in NRPN report: " + path);
                }
                if (!tests.isEmpty() || !learned.isEmpty()) {
                    retained.add(control);
                }
            }
            report.add("controls", retained);
            return report;
        } catch (JsonParseException | IllegalStateException | NullPointerException
                | ClassCastException | NumberFormatException e) {
            throw new IOException("Invalid NRPN report: " + path, e);
        }
    }

    static boolean addEvidence(JsonObject report, Reference reference) {
        if (reference.tests.isEmpty() && reference.learned.isEmpty()) return false;
        JsonArray controls = report.getAsJsonArray("controls");
        for (JsonElement element : controls) {
            JsonObject control = element.getAsJsonObject();
            if (reference.label.equals(control.get("label").getAsString())
                    && reference.tableStart == control.get("tableStart").getAsInt()
                    && reference.tableEnd == control.get("tableEnd").getAsInt()) {
                control.getAsJsonArray("tests").addAll(JSON.toJsonTree(reference.tests).getAsJsonArray());
                control.getAsJsonArray("learned").addAll(JSON.toJsonTree(reference.learned).getAsJsonArray());
                return true;
            }
        }
        controls.add(JSON.toJsonTree(reference));
        return true;
    }

    static void saveReport(Path path, JsonObject report, boolean create) throws IOException {
        String json = JSON.toJson(report) + System.lineSeparator();
        if (create) {
            Files.writeString(path, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } else {
            Path absolute = path.toAbsolutePath();
            Path temp = Files.createTempFile(absolute.getParent(), ".nrpn-report-", ".tmp");
            try {
                Files.writeString(temp, json, StandardCharsets.UTF_8);
                try {
                    Files.move(temp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temp);
            }
        }
    }

    static final class CaptureReceiver implements Receiver {
        private final List<Cc> messages = new ArrayList<>();
        private boolean capturing;

        @Override public synchronized void send(MidiMessage message, long timestamp) {
            if (capturing && message instanceof ShortMessage shortMessage
                    && shortMessage.getCommand() == ShortMessage.CONTROL_CHANGE) {
                messages.add(new Cc(shortMessage.getChannel(),
                        shortMessage.getData1(), shortMessage.getData2()));
            }
        }
        synchronized void start() { messages.clear(); capturing = true; }
        synchronized List<Cc> stop() { capturing = false; return List.copyOf(messages); }
        @Override public synchronized void close() { capturing = false; messages.clear(); }
    }

    record Cc(int channel, int controller, int value) {}

    private static final class ChannelState {
        int msb = -1;
        int lsb = -1;
        boolean nrpn;
        int pendingAddress;
        int pendingMsb = -1;
        int pendingLsb = -1;
    }

    static final class Observation {
        final int midiChannel;
        final int address;
        final int msb;
        final int lsb;
        Integer minCc6;
        Integer maxCc6;
        Integer minCc38;
        Integer maxCc38;
        Integer min14;
        Integer max14;

        Observation(int midiChannel, int address) {
            this.midiChannel = midiChannel;
            this.address = address;
            this.msb = address >> 7;
            this.lsb = address & 0x7F;
        }

        void add(int dataMsb, int dataLsb) {
            minCc6 = minCc6 == null ? dataMsb : Math.min(minCc6, dataMsb);
            maxCc6 = maxCc6 == null ? dataMsb : Math.max(maxCc6, dataMsb);
            if (dataLsb >= 0) {
                addLsb(dataLsb);
                int value = (dataMsb << 7) | dataLsb;
                min14 = min14 == null ? value : Math.min(min14, value);
                max14 = max14 == null ? value : Math.max(max14, value);
            }
        }

        void addLsb(int value) {
            minCc38 = minCc38 == null ? value : Math.min(minCc38, value);
            maxCc38 = maxCc38 == null ? value : Math.max(maxCc38, value);
        }

        String summary() {
            return String.format("channel=%d address=%02X:%02X CC6=%s..%s CC38=%s..%s 14-bit=%s..%s",
                    midiChannel, msb, lsb, minCc6, maxCc6, minCc38, maxCc38, min14, max14);
        }
    }

    static final class Learning {
        final String status = "observed";
        final List<Observation> observations;
        final List<ObservedRange> ranges;

        Learning(List<Observation> observations) {
            this.observations = List.copyOf(observations);
            Map<Integer, List<Observation>> channels = new TreeMap<>();
            observations.forEach(observation -> channels
                    .computeIfAbsent(observation.midiChannel, ignored -> new ArrayList<>())
                    .add(observation));
            this.ranges = channels.entrySet().stream()
                    .map(entry -> new ObservedRange(entry.getKey(), entry.getValue()))
                    .toList();
        }
    }

    static final class ObservedRange {
        final int midiChannel;
        final int lowestObservedAddress;
        final int highestObservedAddress;
        final int observedAddressCount;
        final String assumption = "highest observed address is the last in the selected range";

        ObservedRange(int channel, List<Observation> observations) {
            midiChannel = channel;
            lowestObservedAddress = observations.stream().mapToInt(o -> o.address).min().orElseThrow();
            highestObservedAddress = observations.stream().mapToInt(o -> o.address).max().orElseThrow();
            observedAddressCount = observations.size();
        }
    }

    static final class Reference {
        final String label;
        final int tableStart;
        final int tableEnd;
        final String status = "reference_candidate";
        final List<Verification> tests = new ArrayList<>();
        final List<Learning> learned = new ArrayList<>();

        Reference(String label, int start, int end) {
            this.label = label;
            this.tableStart = start;
            this.tableEnd = end;
        }
    }

    private record Verification(int tableInstance, int midiChannel, int address,
            int cc6, Integer cc38, String status) {}

}
