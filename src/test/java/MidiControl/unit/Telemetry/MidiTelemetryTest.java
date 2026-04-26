package MidiControl.unit.Telemetry;

import MidiControl.Telemetry.MidiTelemetry;
import MidiControl.Telemetry.TelemetryData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MidiTelemetryTest {

    @Test
    public void sentAndReceivedUpdateSessionPeakInflight() {
        MidiTelemetry t = new MidiTelemetry(new FakeSendEngine(75));

        t.sent(10);
        t.sent(5);
        t.received(8);

        assertEquals(15, t.getPeakInflightSession());
    }

    @Test
    public void droppedUpdatesSessionPeak() {
        MidiTelemetry t = new MidiTelemetry(new FakeSendEngine(60));

        t.dropped(2);
        t.dropped(1);
        t.dropped(5);

        assertEquals(5, t.getPeakDroppedSession());
    }

    @Test
    public void snapshotUsesPeriodPeaksAndResetsPeriodOnly() {
        FakeSendEngine engine = new FakeSendEngine(42);
        MidiTelemetry t = new MidiTelemetry(engine);

        t.sent(100);
        t.received(40);
        t.dropped(12);

        TelemetryData s1 = t.snapshotAndResetPeriod();

        assertEquals(100, s1.getInFlight());
        assertEquals(12, s1.getMessagesDropped());
        assertEquals(42, s1.getRemainingCapacity());

        t.sent(5);
        t.dropped(2);

        TelemetryData s2 = t.snapshotAndResetPeriod();

        assertEquals(65, s2.getInFlight());
        assertEquals(2, s2.getMessagesDropped());

        assertEquals(100, t.getPeakInflightSession());
        assertEquals(12, t.getPeakDroppedSession());
    }

    @Test
    public void snapshotAveragesAreNonNegativeAndCombinedIsAtLeastComponents() {
        MidiTelemetry t = new MidiTelemetry(new FakeSendEngine(100));

        t.sent(20);
        t.received(10);

        TelemetryData s = t.snapshotAndResetPeriod();

        assertTrue(s.getAvgOut() >= 0);
        assertTrue(s.getAvgIn() >= 0);
        assertTrue(s.getAvgCombined() >= 0);
        assertTrue(s.getAvgCombined() >= s.getAvgOut());
        assertTrue(s.getAvgCombined() >= s.getAvgIn());
    }

    @Test
    public void snapshotReadsSysexRemainingPercentEachTime() {
        FakeSendEngine engine = new FakeSendEngine(33);
        MidiTelemetry t = new MidiTelemetry(engine);

        TelemetryData s1 = t.snapshotAndResetPeriod();
        assertEquals(33, s1.getRemainingCapacity());

        engine.setRemainingPercent(80);

        TelemetryData s2 = t.snapshotAndResetPeriod();
        assertEquals(80, s2.getRemainingCapacity());
    }
}