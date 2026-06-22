package MidiControl.TestUtilities;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;

public class NrpnLearnMain {

    public static void main(String[] args) throws Exception {
        try (Scanner scanner = new Scanner(System.in)) {
            MidiDevice input = selectInputDevice(scanner);
            input.open();

            Transmitter transmitter = input.getTransmitter();
            CaptureReceiver receiver = new CaptureReceiver();
            transmitter.setReceiver(receiver);

            List<LearnResult> results = new ArrayList<>();

            while (true) {
                System.out.println();
                System.out.print("Learn a control range? Enter label, or 'c' to finish: ");
                String label = scanner.nextLine().trim();

                if (label.equalsIgnoreCase("c")) {
                    break;
                }

                if (label.isBlank()) {
                    continue;
                }

                System.out.print("Canonical prefix, e.g. kInputFader.kFader: ");
                String canonicalPrefix = scanner.nextLine().trim();

                if (canonicalPrefix.isBlank()) {
                    System.out.println("Canonical prefix cannot be blank.");
                    continue;
                }

                int instances = askInt(scanner, "How many instances?");

                LearnOutcome outcome = learnWithRetry(
                    scanner,
                    receiver,
                    label,
                    canonicalPrefix,
                    instances
                );

                if (outcome.finish) {
                    break;
                }

                if (outcome.result != null) {
                    results.add(outcome.result);
                }
            }

            transmitter.close();
            input.close();

            printReport(results);
            writeReport(results, "nrpn_learn_report.txt");

            System.out.println();
            System.out.println("Report written to nrpn_learn_report.txt");
        }
    }

    private static LearnOutcome learnWithRetry(
        Scanner scanner,
        CaptureReceiver receiver,
        String label,
        String canonicalPrefix,
        int instances
    ) {
        while (true) {
            LearnResult result = learnLinearControl(
                scanner,
                receiver,
                label,
                canonicalPrefix,
                instances
            );

            System.out.println();
            System.out.println(result.toReport());
            System.out.println();
            System.out.print("Press ENTER to accept, 'r' to retry, or 'c' to finish: ");

            String action = scanner.nextLine().trim();

            if (action.equalsIgnoreCase("r")) {
                continue;
            }

            if (action.equalsIgnoreCase("c")) {
                return new LearnOutcome(null, true);
            }

            return new LearnOutcome(result, false);
        }
    }

    private static LearnResult learnLinearControl(
        Scanner scanner,
        CaptureReceiver receiver,
        String label,
        String canonicalPrefix,
        int instances
    ) {
        System.out.println();
        System.out.println(label);

        CaptureStats first = captureStats(
            scanner,
            receiver,
            "Move the starting control through its full useful range."
        );

        CaptureStats last = captureStats(
            scanner,
            receiver,
            "Move the final control through its full useful range."
        );

        int stride = calculateStride(first.bestPair, last.bestPair, instances);
        String mode = inferMode(first, last);

        return new LearnResult(
            label,
            canonicalPrefix,
            instances,
            first,
            last,
            stride,
            mode
        );
    }

    private static CaptureStats captureStats(
        Scanner scanner,
        CaptureReceiver receiver,
        String prompt
    ) {
        System.out.println();
        System.out.println(prompt);
        System.out.println("Press ENTER when done.");

        receiver.clear();
        scanner.nextLine();

        List<Cc> captured = receiver.snapshot();
        List<NrpnEvent> events = parseNrpnEvents(captured);
        CaptureStats stats = analyse(events);

        System.out.println("Captured CC messages: " + captured.size());
        System.out.println("Parsed NRPN events: " + events.size());
        System.out.println(stats.shortSummary());

        return stats;
    }

    private static MidiDevice selectInputDevice(Scanner scanner) throws Exception {
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

        System.out.println("MIDI IN devices:");

        for (int i = 0; i < infos.length; i++) {
            MidiDevice device = MidiSystem.getMidiDevice(infos[i]);

            if (device.getMaxTransmitters() != 0) {
                System.out.println(
                    "[" + i + "] " +
                    infos[i].getName() + " | " +
                    infos[i].getDescription()
                );
            }
        }

        int selected = askInt(scanner, "Select MIDI IN device index:");
        return MidiSystem.getMidiDevice(infos[selected]);
    }

    private static int askInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt + " ");

            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }

    private static List<NrpnEvent> parseNrpnEvents(List<Cc> ccs) {
        List<NrpnEvent> events = new ArrayList<>();

        int currentMsb = -1;
        int currentLsb = -1;
        Integer currentDataMsb = null;

        for (Cc cc : ccs) {
            if (cc.controller == 99) {
                currentMsb = cc.value;
            } else if (cc.controller == 98) {
                currentLsb = cc.value;
            } else if (cc.controller == 6) {
                if (currentMsb >= 0 && currentLsb >= 0) {
                    currentDataMsb = cc.value;
                    events.add(new NrpnEvent(currentMsb, currentLsb, currentDataMsb, null));
                }
            } else if (cc.controller == 38) {
                if (currentMsb >= 0 && currentLsb >= 0 && currentDataMsb != null) {
                    events.add(new NrpnEvent(currentMsb, currentLsb, currentDataMsb, cc.value));
                }
            }
        }

        return events;
    }

    private static CaptureStats analyse(List<NrpnEvent> events) {
        if (events.isEmpty()) {
            return CaptureStats.empty();
        }

        Map<NrpnPair, Integer> counts = new HashMap<>();

        int minCc6 = Integer.MAX_VALUE;
        int maxCc6 = Integer.MIN_VALUE;
        int min14 = Integer.MAX_VALUE;
        int max14 = Integer.MIN_VALUE;
        boolean hasCc38 = false;

        for (NrpnEvent event : events) {
            NrpnPair pair = new NrpnPair(event.msb, event.lsb);

            counts.put(pair, counts.getOrDefault(pair, 0) + 1);

            minCc6 = Math.min(minCc6, event.dataMsb);
            maxCc6 = Math.max(maxCc6, event.dataMsb);

            if (event.dataLsb != null) {
                hasCc38 = true;
                int value14 = event.value14();
                min14 = Math.min(min14, value14);
                max14 = Math.max(max14, value14);
            }
        }

        NrpnPair bestPair = counts.entrySet()
            .stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(NrpnPair.missing());

        return new CaptureStats(
            bestPair,
            counts.size(),
            minCc6,
            maxCc6,
            hasCc38 ? min14 : -1,
            hasCc38 ? max14 : -1,
            hasCc38
        );
    }

    private static int calculateStride(NrpnPair first, NrpnPair last, int instances) {
        if (instances <= 1 || first.isMissing() || last.isMissing()) {
            return 0;
        }

        int firstOrdinal = first.ordinal();
        int lastOrdinal = last.ordinal();

        return Math.round((lastOrdinal - firstOrdinal) / (float) (instances - 1));
    }

    private static String inferMode(CaptureStats... stats) {
        boolean hasCc38 = false;
        int max14 = -1;

        for (CaptureStats stat : stats) {
            hasCc38 = hasCc38 || stat.hasCc38;
            max14 = Math.max(max14, stat.max14);
        }

        if (hasCc38 && max14 > 1023) {
            return "NRPN_14BIT";
        }

        if (hasCc38) {
            return "NRPN_RAW_SPLIT";
        }

        return "CC6_ONLY";
    }

    private static void printReport(List<LearnResult> results) {
        System.out.println();

        for (LearnResult result : results) {
            System.out.println(result.toReport());
            System.out.println();
        }
    }

    private static void writeReport(List<LearnResult> results, String fileName) throws Exception {
        try (PrintWriter out = new PrintWriter(new FileWriter(fileName))) {
            for (LearnResult result : results) {
                out.println(result.toReport());
                out.println();
            }
        }
    }

    private static String hexByte(int value) {
        return value < 0 ? "<missing>" : String.format("0x%02X", value & 0x7F);
    }

    private static final class CaptureReceiver implements Receiver {
        private final CopyOnWriteArrayList<Cc> events = new CopyOnWriteArrayList<>();

        @Override
        public void send(MidiMessage message, long timeStamp) {
            if (!(message instanceof ShortMessage sm)) {
                return;
            }

            if (sm.getCommand() != ShortMessage.CONTROL_CHANGE) {
                return;
            }

            Cc cc = new Cc(sm.getChannel(), sm.getData1(), sm.getData2());
            events.add(cc);

            System.out.println("IN " + cc);
        }

        @Override
        public void close() {
            clear();
        }

        void clear() {
            events.clear();
        }

        List<Cc> snapshot() {
            return new ArrayList<>(events);
        }
    }

    private record Cc(int channel, int controller, int value) {
        @Override
        public String toString() {
            return "ch=" + channel +
                " cc=" + controller +
                " value=" + value +
                " hex=" + String.format(
                    "%02X %02X %02X",
                    0xB0 | (channel & 0x0F),
                    controller & 0x7F,
                    value & 0x7F
                );
        }
    }

    private record NrpnEvent(int msb, int lsb, int dataMsb, Integer dataLsb) {
        int value14() {
            return dataLsb == null
                ? -1
                : ((dataMsb & 0x7F) << 7) | (dataLsb & 0x7F);
        }
    }

    private record NrpnPair(int msb, int lsb) {
        static NrpnPair missing() {
            return new NrpnPair(-1, -1);
        }

        boolean isMissing() {
            return msb < 0 || lsb < 0;
        }

        int ordinal() {
            if (isMissing()) {
                return -1;
            }

            return ((msb & 0x7F) << 7) | (lsb & 0x7F);
        }

        String msbHex() {
            return hexByte(msb);
        }

        String lsbHex() {
            return hexByte(lsb);
        }

        String summary() {
            return "msb=" + msbHex() + " lsb=" + lsbHex();
        }
    }

    private record CaptureStats(
        NrpnPair bestPair,
        int uniquePairs,
        int minCc6,
        int maxCc6,
        int min14,
        int max14,
        boolean hasCc38
    ) {
        static CaptureStats empty() {
            return new CaptureStats(
                NrpnPair.missing(),
                0,
                -1,
                -1,
                -1,
                -1,
                false
            );
        }

        String shortSummary() {
            return "bestPair=" + bestPair.summary() +
                " uniquePairs=" + uniquePairs +
                " cc6Range=" + minCc6 + ".." + maxCc6 +
                " cc38=" + hasCc38 +
                " value14Range=" + min14 + ".." + max14;
        }
    }

    private record LearnOutcome(LearnResult result, boolean finish) {}

    private record LearnResult(
        String label,
        String canonicalPrefix,
        int instances,
        CaptureStats first,
        CaptureStats last,
        int stride,
        String nrpnMode
    ) {
        String toReport() {
            return String.join(
                System.lineSeparator(),
                label,
                "canonical_prefix=" + canonicalPrefix,
                "instances=" + instances,
                "start_nrpn_msb=" + first.bestPair.msbHex(),
                "start_nrpn_lsb=" + first.bestPair.lsbHex(),
                "last_nrpn_msb=" + last.bestPair.msbHex(),
                "last_nrpn_lsb=" + last.bestPair.lsbHex(),
                "nrpn_stride=" + stride,
                "value_mode=" + nrpnMode,
                "first=" + first.shortSummary(),
                "last=" + last.shortSummary(),
                "generator_hint=generate_block(" +
                    "start_msb=" + first.bestPair.msbHex() +
                    ", start_lsb=" + first.bestPair.lsbHex() +
                    ", canonical_prefix=\"" + canonicalPrefix + "\"" +
                    ", instances=" + instances +
                    ", stride=" + stride +
                    ", value_mode=\"" + nrpnMode + "\"" +
                    ")"
            );
        }
    }
}