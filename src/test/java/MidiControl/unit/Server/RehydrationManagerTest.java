package MidiControl.unit.Server;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;
import MidiControl.Controls.SourceAllInstances;
import MidiControl.Routing.OutputRequestSender;
import MidiControl.Server.RehydrationManager;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexParser;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RehydrationManagerTest {

    // ------------------------------------------------------------
    // Fake OutputRouter for testing
    // ------------------------------------------------------------
    static class FakeOutputRouter implements OutputRequestSender {
        String lastRequestedId = null;
        int callCount = 0;

        @Override
        public void applyRequest(String canonicalId) {
            this.lastRequestedId = canonicalId;
            this.callCount++;
        }
    }
    // ------------------------------------------------------------
    // Fake Registry for testing
    // (We don't need real instances yet — just a stub)
    // ------------------------------------------------------------
    static class FakeRegistry implements SourceAllInstances{
        public Collection<ControlInstance> getAllInstances() {
            return List.of();
        }
    }

    // ------------------------------------------------------------
    // Fake scheduler (no tasks needed yet)
    // ------------------------------------------------------------
    static class FakeScheduler extends java.util.concurrent.ScheduledThreadPoolExecutor {
        FakeScheduler() { super(1); }
        @Override
        public java.util.concurrent.ScheduledFuture<?> schedule(Runnable r, long delay, java.util.concurrent.TimeUnit unit) {
            // Do nothing for now — timeout tests come later
            return null;
        }
    }

    // ------------------------------------------------------------
    // The test
    // ------------------------------------------------------------
    @Test
    void request_callsOutputRouterApplyRequest() {
        // Arrange
        FakeOutputRouter fakeRouter = new FakeOutputRouter();
        FakeRegistry fakeRegistry = new FakeRegistry();
        FakeScheduler fakeScheduler = new FakeScheduler();

        RehydrationManager mgr = new RehydrationManager(fakeRouter, fakeRegistry, fakeScheduler);

        // Act
        mgr.request("kInputAUX.kAUX1Level.0");

        // Assert
        assertEquals("kInputAUX.kAUX1Level.0", fakeRouter.lastRequestedId);
        assertEquals(1, fakeRouter.callCount);
    }

    @Test
    void request_addsCanonicalIdToPending() {
        FakeOutputRouter fakeRouter = new FakeOutputRouter();
        FakeRegistry fakeRegistry = new FakeRegistry();
        FakeScheduler fakeScheduler = new FakeScheduler();

        RehydrationManager mgr = new RehydrationManager(fakeRouter, fakeRegistry, fakeScheduler);

        mgr.request("foo.bar.0");

        assertTrue(mgr.isPending("foo.bar.0"));
    }

    @Test
    void onControlUpdated_clearsPending() {
        FakeOutputRouter fakeRouter = new FakeOutputRouter();
        FakeRegistry fakeRegistry = new FakeRegistry();
        FakeScheduler fakeScheduler = new FakeScheduler();

        RehydrationManager mgr = new RehydrationManager(fakeRouter, fakeRegistry, fakeScheduler);

        mgr.request("foo.bar.0");
        mgr.onControlUpdated("foo.bar.0");

        assertFalse(mgr.isPending("foo.bar.0"));
    }

    static class ImmediateScheduler implements ScheduledExecutorService {
        @Override
        public ScheduledFuture<?> schedule(Runnable r, long delay, TimeUnit unit) {
            r.run();
            return null;}
        @Override
        public void shutdown() {
            throw new UnsupportedOperationException("Unimplemented method 'shutdown'");}
        @Override
        public List<Runnable> shutdownNow() {
            throw new UnsupportedOperationException("Unimplemented method 'shutdownNow'");}
        @Override
        public boolean isShutdown() {
            throw new UnsupportedOperationException("Unimplemented method 'isShutdown'");}
        @Override
        public boolean isTerminated() {
            throw new UnsupportedOperationException("Unimplemented method 'isTerminated'");}
        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            throw new UnsupportedOperationException("Unimplemented method 'awaitTermination'");}
        @Override
        public <T> Future<T> submit(Callable<T> task) {
            throw new UnsupportedOperationException("Unimplemented method 'submit'");}
        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            throw new UnsupportedOperationException("Unimplemented method 'submit'");}
        @Override
        public Future<?> submit(Runnable task) {
            throw new UnsupportedOperationException("Unimplemented method 'submit'");}
        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            throw new UnsupportedOperationException("Unimplemented method 'invokeAll'"); }
        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException {
            throw new UnsupportedOperationException("Unimplemented method 'invokeAll'");}
        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
                throws InterruptedException, ExecutionException {
            throw new UnsupportedOperationException("Unimplemented method 'invokeAny'");}
        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            throw new UnsupportedOperationException("Unimplemented method 'invokeAny'");}
        @Override
        public void execute(Runnable command) {
            throw new UnsupportedOperationException("Unimplemented method 'execute'");}
        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            throw new UnsupportedOperationException("Unimplemented method 'schedule'");}
        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            throw new UnsupportedOperationException("Unimplemented method 'scheduleAtFixedRate'");}
        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
                TimeUnit unit) {throw new UnsupportedOperationException("Unimplemented method 'scheduleWithFixedDelay'");}
    }

    @Test
    void timeout_removesPending() {
        FakeOutputRouter fakeRouter = new FakeOutputRouter();
        FakeRegistry fakeRegistry = new FakeRegistry();
        ImmediateScheduler scheduler = new ImmediateScheduler();

        RehydrationManager mgr = new RehydrationManager(fakeRouter, fakeRegistry, scheduler);

        mgr.request("foo.bar.0");

        assertFalse(mgr.isPending("foo.bar.0"));
    }


}

