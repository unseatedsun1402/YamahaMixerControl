package MidiControl.Telemetry;

import java.util.concurrent.*;

public final class TelemetryPublisher {

    private final ScheduledExecutorService scheduler;
    private final MidiTelemetry telemetry;
    private final TelemetryListener listener;

    public TelemetryPublisher(MidiTelemetry telemetry,
                              TelemetryListener listener,
                              long periodSeconds) {
        this.telemetry = telemetry;
        this.listener = listener;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "midi-telemetry-publisher");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(
                this::publish,
                periodSeconds,
                periodSeconds,
                TimeUnit.SECONDS
        );
    }

    private void publish() {
        TelemetryData dto = telemetry.snapshotAndResetPeriod();
        listener.onTelemetry(dto.toJson());
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
