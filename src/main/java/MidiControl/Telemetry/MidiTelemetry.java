package MidiControl.Telemetry;

import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import MidiControl.MidiDeviceManager.MidiSendEngine;
import MidiControl.Server.Rehydration.RehydrationMetrics;

public final class MidiTelemetry {

    private final LongAdder bytesOut = new LongAdder();
    private final LongAdder bytesIn = new LongAdder();
    private final LongAdder droppedBytes = new LongAdder();

    private final LongAdder currentInflightBytes = new LongAdder();

    private volatile long periodPeakInflightBytes;
    private volatile long sessionPeakInflightBytes;

    private volatile long periodPeakDroppedBytes;
    private volatile long sessionPeakDroppedBytes;

    private volatile long periodStartEpochSec;
    private volatile MidiSendEngine sendEngine;
    private volatile TelemetryListener telemetryListener = TelemetryListener.NO_OP;
    private final RehydrationMetrics rehydrationMetrics;


    private static Logger logger = Logger.getLogger(MidiTelemetry.class.getName());

    
    public MidiTelemetry(MidiSendEngine mse, RehydrationMetrics rehydrationMetrics) {
        logger.info("Telemetry engine started");
        this.periodStartEpochSec = now();
        this.sendEngine = mse;
        this.rehydrationMetrics = rehydrationMetrics;
    }



    public MidiTelemetry(MidiSendEngine mse) {
        this(mse, null);
    }

    public void sent(int bytes) {
        bytesOut.add(bytes);
        currentInflightBytes.add(bytes);
        long v = currentInflightBytes.sum();
        periodPeakInflightBytes = Math.max(periodPeakInflightBytes, v);
        sessionPeakInflightBytes = Math.max(sessionPeakInflightBytes, v);
    }

    public void received(int bytes) {
        bytesIn.add(bytes);
        currentInflightBytes.add(-bytes);
    }

    public void dropped(int bytes) {
        droppedBytes.add(bytes);
        periodPeakDroppedBytes = Math.max(periodPeakDroppedBytes, bytes);
        sessionPeakDroppedBytes = Math.max(sessionPeakDroppedBytes, bytes);
    }

    public TelemetryData snapshotAndResetPeriod() {
        long now = now();
        long elapsed = Math.max(1, now - periodStartEpochSec);

        TelemetryData dto = new TelemetryData();
        dto.setTimeStamp(now);
        dto.setAvgOut((int) (bytesOut.sum() / elapsed));
        dto.setAvgIn((int) (bytesIn.sum() / elapsed));
        dto.setAvgCombined((int) ((bytesOut.sum() + bytesIn.sum()) / elapsed));
        dto.setInFlightBytes((int) periodPeakInflightBytes);
        dto.setDroppedMessages((int) periodPeakDroppedBytes);
        dto.setSysexQueueCapacity(sendEngine.getSysexQueueRemainingPercent());

        if (rehydrationMetrics != null) {
            dto.setInflightTransactions(
                    rehydrationMetrics.getInflightTransactionCount()
            );
            dto.setTimedOutTransactions(
                    rehydrationMetrics.getTimedOutTransactionCount()
            );
        }

        bytesOut.reset();
        bytesIn.reset();
        droppedBytes.reset();
        periodPeakInflightBytes = 0;
        periodPeakDroppedBytes = 0;
        periodStartEpochSec = now;
        telemetryListener.onJson(new Gson().fromJson(dto.toJsonString(),JsonObject.class) );
        return dto;
    }

    public long getPeakInflightSession() {
        return sessionPeakInflightBytes;
    }

    public long getPeakDroppedSession() {
        return sessionPeakDroppedBytes;
    }

    private static long now() {
        return java.time.Instant.now().getEpochSecond();
    }

    public void setTelemetryListener(TelemetryListener listener){
        this.telemetryListener = listener;
        logger.info("Set Midi Telemetry listener");
    }
}