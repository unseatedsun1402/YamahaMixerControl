package MidiControl.Server.Rehydration;

public final class RehydrationTelemetry {

    private final RehydrationManager manager;

    public RehydrationTelemetry(RehydrationManager manager) {
        this.manager = manager;
    }

    public int getOutstandingRequests() {
        return manager.getOutstandingRequests();
    }

    public int getPeriodTimedOutRequests() {
        return manager.getPeriodTimedOutRequestsTotal();
    }

    public long getAvgRequestRttMs() {
        return manager.getAvgRequestRttMsAndReset();
    }

    public boolean getMeterRequestStatus(){
        return manager.getMeterRequestsActive();
    }
}