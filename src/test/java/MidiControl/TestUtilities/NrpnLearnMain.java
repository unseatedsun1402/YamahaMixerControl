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

            int inputCount = askInt(scanner, "How many input channels?");
            int sendCount = askInt(scanner, "How many sends/mixes/auxes?");

            List<LearnResult> results = new ArrayList<>();

            results.add(learnLinearControl(
                scanner,
                receiver,
                "Input fader",
                "kInputFader.kFader",
                inputCount,
                "Move input fader 1 through its full useful range.",
                "Move the final input fader through its full useful range."
            ));

            results.add(learnLinearControl(
                scanner,
                receiver,
                "Input pan",
                "kInputPan.kChannelPan",
                inputCount,
                "Move input pan 1 through its full useful range.",
                "Move the final input pan through its full useful range."
            ));

            results.add(learnLinearControl(
                scanner,
                receiver,
                "Input compressor threshold",
                "kInputDynamics1.kThreshold",
                inputCount,
                "Move input compressor threshold for channel 1 through its full useful range.",
                "Move input compressor threshold for the final channel through its full useful range."
            ));

            results.add(learnLinearControl(
                scanner,
                receiver,
                "Input on/mute",
                "kInputOn.kChannelOn",
                inputCount,
                "Toggle input on/mute for channel 1 several times.",
                "Toggle input on/mute for the final channel several times."
            ));

            results.add(learnSendControl(
                scanner,
                receiver,
                "Input send level",
                "kInputToSend.kSendLevel",
                inputCount,
                sendCount
            ));

            transmitter.close();
            input.close();

            printReport(results);
            writeReport(results, "nrpn_learn_report.txt");

            System.out.println();
            System.out.println("Report written to nrpn_learn_report.txt");
        }
    }

    private static LearnResult learnLinearControl(
        Scanner scanner,
        CaptureReceiver receiver,
        String label,
        String canonicalPrefix,
        int instances,
        String firstPrompt,
        String lastPrompt
    ) {
        System.out.println();
        System.out.println(label);

        CaptureStats first = captureStats(scanner, receiver, firstPrompt);
        CaptureStats last = captureStats(scanner, receiver, lastPrompt);

        int firstAddress = first.bestAddress;
        int lastAddress = last.bestAddress;

        int stride = instances > 1 && firstAddress >= 0 && lastAddress >= 0
            ? Math.round((lastAddress - firstAddress) / (float) (instances - 1))
            : 0;

        return LearnResult.linear(
            label,
            canonicalPrefix,
            instances,
            first,
            last,
            firstAddress,
            lastAddress,
            stride,
            inferMode(first, last)
        );
    }

    private static LearnResult learnSendControl(
        Scanner scanner,
        CaptureReceiver receiver,
        String label,
        String canonicalPrefix,
        int inputCount,
        int sendCount
    ) {
        System.out.println();
        System.out.println(label);

        CaptureStats send1Input1 = captureStats(
            scanner,
            receiver,
            "Move send 1 level for input 1 through its full useful range."
        );

        CaptureStats send1LastInput = captureStats(
            scanner,
            receiver,
            "Move send 1 level for the final input through its full useful range."
        );

        CaptureStats lastSendInput1 = captureStats(
            scanner,
            receiver,
            "Move final send level for input 1 through its full useful range."
        );

        CaptureStats lastSendLastInput = captureStats(
            scanner,
            receiver,
            "Move final send level for the final input through its full useful range."
        );

        int inputStride = inputCount > 1 &&
            send1Input1.bestAddress >= 0 &&
            send1LastInput.bestAddress >= 0
                ? Math.round((send1LastInput.bestAddress - send1Input1.bestAddress) / (float) (inputCount - 1))
                : 0;

        int sendStride = sendCount > 1 &&
            send1Input1.bestAddress >= 0 &&
            lastSendInput1.bestAddress >= 0
                ? Math.round((lastSendInput1.bestAddress - send1Input1.bestAddress) / (float) (sendCount - 1))
                : 0;

        return LearnResult.send(
            label,
            canonicalPrefix,
            inputCount,
            sendCount,
            send1Input1,
            send1LastInput,
            lastSendInput1,
            lastSendLastInput,
            inputStride,
            sendStride,
            inferMode(send1Input1, send1LastInput, lastSendInput1, lastSendLastInput)
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
        System.out.print(prompt + " ");
        return Integer.parseInt(scanner.nextLine().trim());
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

        Map<Integer, Integer> counts = new HashMap<>();

        int minAddress = Integer.MAX_VALUE;
        int maxAddress = Integer.MIN_VALUE;
        int minCc6 = Integer.MAX_VALUE;
        int maxCc6 = Integer.MIN_VALUE;
        int min14 = Integer.MAX_VALUE;
        int max14 = Integer.MIN_VALUE;
        boolean hasCc38 = false;

        for (NrpnEvent event : events) {
            int address = event.address();

            counts.put(address, counts.getOrDefault(address, 0) + 1);

            minAddress = Math.min(minAddress, address);
            maxAddress = Math.max(maxAddress, address);
            minCc6 = Math.min(minCc6, event.dataMsb);
            maxCc6 = Math.max(maxCc6, event.dataMsb);

            if (event.dataLsb != null) {
                hasCc38 = true;
                int value14 = event.value14();
                min14 = Math.min(min14, value14);
                max14 = Math.max(max14, value14);
            }
        }

        int bestAddress = counts.entrySet()
            .stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(-1);

        return new CaptureStats(
            bestAddress,
            minAddress,
            maxAddress,
            minCc6,
            maxCc6,
            hasCc38 ? min14 : -1,
            hasCc38 ? max14 : -1,
            hasCc38
        );
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

    private static String hex16(int value) {
        return value < 0 ? "<missing>" : String.format("0x%04X", value & 0xFFFF);
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
        int address() {
            return ((msb & 0x7F) << 8) | (lsb & 0x7F);
        }

        int value14() {
            return dataLsb == null
                ? -1
                : ((dataMsb & 0x7F) << 7) | (dataLsb & 0x7F);
        }
    }

    private record CaptureStats(
        int bestAddress,
        int minAddress,
        int maxAddress,
        int minCc6,
        int maxCc6,
        int min14,
        int max14,
        boolean hasCc38
    ) {
        static CaptureStats empty() {
            return new CaptureStats(-1, -1, -1, -1, -1, -1, -1, false);
        }

        String shortSummary() {
            return "bestAddress=" + hex16(bestAddress) +
                " addressRange=" + hex16(minAddress) + ".." + hex16(maxAddress) +
                " cc6Range=" + minCc6 + ".." + maxCc6 +
                " cc38=" + hasCc38 +
                " value14Range=" + min14 + ".." + max14;
        }
    }

    private record LearnResult(
        String kind,
        String label,
        String canonicalPrefix,
        int instances,
        int sendCount,
        CaptureStats first,
        CaptureStats last,
        CaptureStats send1Input1,
        CaptureStats send1LastInput,
        CaptureStats lastSendInput1,
        CaptureStats lastSendLastInput,
        int firstAddress,
        int lastAddress,
        int addressStride,
        int inputStride,
        int sendStride,
        String nrpnMode
    ) {
        static LearnResult linear(
            String label,
            String canonicalPrefix,
            int instances,
            CaptureStats first,
            CaptureStats last,
            int firstAddress,
            int lastAddress,
            int stride,
            String mode
        ) {
            return new LearnResult(
                "linear",
                label,
                canonicalPrefix,
                instances,
                0,
                first,
                last,
                null,
                null,
                null,
                null,
                firstAddress,
                lastAddress,
                stride,
                0,
                0,
                mode
            );
        }

        static LearnResult send(
            String label,
            String canonicalPrefix,
            int inputCount,
            int sendCount,
            CaptureStats send1Input1,
            CaptureStats send1LastInput,
            CaptureStats lastSendInput1,
            CaptureStats lastSendLastInput,
            int inputStride,
            int sendStride,
            String mode
        ) {
            return new LearnResult(
                "send",
                label,
                canonicalPrefix,
                inputCount,
                sendCount,
                null,
                null,
                send1Input1,
                send1LastInput,
                lastSendInput1,
                lastSendLastInput,
                send1Input1.bestAddress,
                send1LastInput.bestAddress,
                inputStride,
                inputStride,
                sendStride,
                mode
            );
        }

        String toReport() {
            if ("send".equals(kind)) {
                return sendReport();
            }

            return linearReport();
        }

        private String linearReport() {
            return String.join(
                System.lineSeparator(),
                label,
                "canonical_prefix=" + canonicalPrefix,
                "instances=" + instances,
                "first_address=" + hex16(firstAddress),
                "last_address=" + hex16(lastAddress),
                "address_stride=" + addressStride,
                "value_mode=" + nrpnMode,
                "first=" + first.shortSummary(),
                "last=" + last.shortSummary(),
                "generator_hint=generate_block(\"" + hex16(firstAddress) + "\", \"" + canonicalPrefix + "\", instances=" + instances + ", value_mode=\"" + nrpnMode + "\")"
            );
        }

        private String sendReport() {
            return String.join(
                System.lineSeparator(),
                label,
                "canonical_prefix=" + canonicalPrefix,
                "input_count=" + instances,
                "send_count=" + sendCount,
                "send1_input1=" + send1Input1.shortSummary(),
                "send1_last_input=" + send1LastInput.shortSummary(),
                "last_send_input1=" + lastSendInput1.shortSummary(),
                "last_send_last_input=" + lastSendLastInput.shortSummary(),
                "input_stride=" + inputStride,
                "send_stride=" + sendStride,
                "value_mode=" + nrpnMode
            );
        }
    }
}