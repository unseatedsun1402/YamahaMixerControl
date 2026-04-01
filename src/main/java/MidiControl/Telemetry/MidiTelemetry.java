package MidiControl.Telemetry;

import java.util.concurrent.atomic.LongAdder;

public final class MidiTelemetry {

    private final LongAdder bytesOut = new LongAdder();
    private final LongAdder bytesIn = new LongAdder();
    private final LongAdder droppedBytes = new LongAdder();

    private final LongAdder inflightBytes = new LongAdder();
    private volatile long peakInflightPeriod;
    private volatile long peakInflightSession;
    private volatile long peakDroppedPeriod;
    private volatile long peakDroppedSession;

    private volatile long periodStartEpochSec;


    public MidiTelemetry() {
        this.periodStartEpochSec = now();
    }

    public void sent(int bytes) {
        bytesOut.add(bytes);
        long v = inflightBytes.sum();
        peakInflightPeriod = Math.max(peakInflightPeriod, v);
        peakInflightSession = Math.max(peakInflightSession, v);
    }

    public void received(int bytes) {
        bytesIn.add(bytes);
        inflightBytes.add(-bytes);
    }

    public void dropped(int bytes) {
        droppedBytes.add(bytes);
        peakDroppedPeriod = Math.max(peakDroppedPeriod, bytes);
        peakDroppedSession = Math.max(peakDroppedSession, bytes);
    }

    public TelemetryData snapshotAndResetPeriod() {
        long now = now();
        long elapsed = Math.max(1, now - periodStartEpochSec);

        TelemetryData dto = new TelemetryData();
        dto.setTimeStamp(now);
        dto.setAvgOut((int) (bytesOut.sum() / elapsed));
        dto.setAvgIn((int) (bytesIn.sum() / elapsed));
        dto.setAvgCombined((int) ((bytesOut.sum() + bytesIn.sum()) / elapsed));
        dto.setInFlight((int) peakInflightPeriod);
        dto.setDroppedMessages((int) peakDroppedPeriod);

        bytesOut.reset();
        bytesIn.reset();
        droppedBytes.reset();
        peakInflightPeriod = 0;
        peakDroppedPeriod = 0;
        periodStartEpochSec = now;

        return dto;
    }

    public long getPeakInflightSession() {
        return peakInflightSession;
    }

    public long getPeakDroppedSession() {
        return peakDroppedSession;
    }

    private static long now() {
        return java.time.Instant.now().getEpochSecond();
    }
}