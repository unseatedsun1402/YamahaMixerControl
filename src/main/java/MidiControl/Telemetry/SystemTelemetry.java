package MidiControl.Telemetry;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import com.google.gson.JsonObject;
import MidiControl.Routing.WebSocketEndpoint;

public final class SystemTelemetry {

    private static final Logger logger = Logger.getLogger(SystemTelemetry.class.getName());

    private volatile long periodSeconds = 5; //default refresh rate
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile MidiTelemetry midiTelemetry;
    private volatile ScheduledExecutorService scheduler;

    public SystemTelemetry() {}

    public void registerMidiTelemetry(MidiTelemetry telemetry) {
        this.midiTelemetry = telemetry;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) return;

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "system-telemetry");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(
            this::tick,
            periodSeconds,
            periodSeconds,
            TimeUnit.SECONDS
        );
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) return;

        ScheduledExecutorService s = scheduler;
        scheduler = null;
        if (s != null) s.shutdownNow();
    }

    private void tick() {
        MidiTelemetry t = midiTelemetry;
        if (t == null) return;

        TelemetryData dto = t.snapshotAndResetPeriod();
        publish(dto.toJsonString());
    }

    public void publish(String json) {
        if (json == null) return;
        WebSocketEndpoint.broadcast(json);
    }

    public void publish(JsonObject json) {
        if (json == null) return;
        WebSocketEndpoint.broadcast(json.toString());
    }
}