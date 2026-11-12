package MidiControl.UserInterface.Meter;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import MidiControl.SysexUtils.SysexParser;

public final class MeterDiagnostics {
    private static final Logger logger = Logger.getLogger(MeterDiagnostics.class.getName());
    private static final long SAMPLE_INTERVAL_MS = 5_000;
    private static final AtomicLong lastCandidate = new AtomicLong();
    private static final AtomicLong lastOtherSysex = new AtomicLong();
    private static final AtomicLong lastParsed = new AtomicLong();
    private static final AtomicLong lastBatch = new AtomicLong();

    private MeterDiagnostics() {}

    public static void requestQueued(String profile, byte[] request) {
        logger.info("[METER-DIAG] queued profile=" + profile + " bytes=" + SysexParser.bytesToHex(request));
    }

    public static void requestSent(String device, byte[] message) {
        if (isMeterRequest(message)) {
            logger.info("[METER-DIAG] handed to MIDI output=" + device
                    + " bytes=" + SysexParser.bytesToHex(message));
        }
    }

    public static void midiInput(byte[] message) {
        if (message.length == 0) return;
        int status = message[0] & 0xFF;
        boolean yamaha = message.length >= 2 && status == 0xF0 && (message[1] & 0xFF) == 0x43;
        boolean candidate = yamaha && ((message.length >= 6 && (message[5] & 0xFF) == 0x21)
                || (message.length >= 7 && (message[6] & 0xFF) == 0x21));
        if (!candidate && !yamaha && status != 0xF7) return;
        if (!sample(candidate ? lastCandidate : lastOtherSysex)) return;
        int shown = Math.min(message.length, 64);
        logger.info("[METER-DIAG] MIDI input " + (candidate ? "meter-like" : "other SysEx")
                + " length=" + message.length
                + " bytes=" + SysexParser.bytesToHex(Arrays.copyOf(message, shown))
                + (shown < message.length ? " ... last=" + String.format("%02X", message[message.length - 1] & 0xFF) : ""));
    }

    public static void parsed(byte[] message, List<Integer> values) {
        if (!sample(lastParsed)) return;
        int minimum = values.stream().mapToInt(Integer::intValue).min().orElse(0);
        int maximum = values.stream().mapToInt(Integer::intValue).max().orElse(0);
        logger.info(String.format("[METER-DIAG] parsed model=%02X category=%02X source=%02X start=%d count=%d min=%d max=%d",
                message[4] & 0xFF, message[6] & 0xFF, message[7] & 0xFF,
                message[8] & 0x7F, values.size(), minimum, maximum));
    }

    public static void broadcast(List<MeterDTO> updates) {
        if (!sample(lastBatch)) return;
        MeterDTO first = updates.get(0);
        logger.info("[METER-DIAG] broadcasting batch count=" + updates.size()
                + " first=" + first.category + "/" + first.source
                + " channel=" + first.offset + " value=" + first.value + " dB=" + first.dB);
    }

    private static boolean isMeterRequest(byte[] message) {
        return message.length >= 6 && (message[0] & 0xFF) == 0xF0
                && (message[1] & 0xFF) == 0x43 && (message[2] & 0xF0) == 0x30
                && (message[3] & 0xFF) == 0x3E && (message[5] & 0xFF) == 0x21;
    }

    private static boolean sample(AtomicLong last) {
        long now = System.currentTimeMillis();
        long previous = last.get();
        return (previous == 0 || now - previous >= SAMPLE_INTERVAL_MS)
                && last.compareAndSet(previous, now);
    }
}
