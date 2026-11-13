package MidiControl.MidiDeviceManager.Simulator;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

final class MeterStream {
    private static final Logger logger = Logger.getLogger(MeterStream.class.getName());
    private static final long DURATION_NANOS = TimeUnit.SECONDS.toNanos(10);
    private static final long PERIOD_NANOS = TimeUnit.SECONDS.toNanos(1) / 30;
    private static final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "SimulatorMeterStreams");
                thread.setDaemon(true);
                return thread;
            });

    private ScheduledFuture<?> future;
    private long generation;

    synchronized void start(Runnable publish) {
        stop();
        long current = generation;
        long expires = System.nanoTime() + DURATION_NANOS;
        publish.run();
        future = scheduler.scheduleAtFixedRate(() -> tick(current, expires, publish),
                PERIOD_NANOS, PERIOD_NANOS, TimeUnit.NANOSECONDS);
    }

    private synchronized void tick(long current, long expires, Runnable publish) {
        if (generation != current) return;
        if (System.nanoTime() - expires >= 0) {
            stop();
            return;
        }
        try {
            publish.run();
        } catch (RuntimeException exception) {
            logger.log(Level.SEVERE, "Simulator meter stream failed", exception);
            stop();
        }
    }

    synchronized void stop() {
        generation++;
        if (future != null) {
            future.cancel(false);
            future = null;
        }
    }
}
