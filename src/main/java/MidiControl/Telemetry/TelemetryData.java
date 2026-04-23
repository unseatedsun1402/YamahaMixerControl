package MidiControl.Telemetry;


public class TelemetryData {
    private int inflight=-1;
    private int averagein=-1;
    private int averageout=-1;
    private int averagecombined=-1;
    private int dropped=-1;
    private long timeStamp;
    private int remainingcapacity = -1;
    private int usedcapacity = -1;
    private int inflightTransactions = -1;
    private int timedOutTransactions = -1;


    public TelemetryData(){
        timeStamp = System.currentTimeMillis();
    }

    public String toJson(){
        return "{\"type\":\"telemetry\",\"payload\":{\"timestamp\":"+this.timeStamp+",\"inflight\":"+this.inflight+",\"dropped\":"+this.dropped+
        ",\"averagein\":"+this.averagein+",\"averageout\":"+this.averageout+",\"averagecombined\":"+averagecombined+",\"remainingcapacity\":"
        +remainingcapacity+",\"usedcapacity\":"+usedcapacity+
        ",\"inflightTransactions\":" + inflightTransactions +
        ",\"timedOutTransactions\":" + timedOutTransactions +
        "} }";
    }

    public long getTimestamp(){return this.timeStamp; }
    public int getInFlight(){return ( this.inflight > -1 ) ? this.inflight : -1; }
    public int getAvgIn(){return ( this.averagein > -1 ) ? this.averagein : -1;}
    public int getAvgOut(){return ( this.averageout > -1 ) ? this.averageout : -1;}
    public int getAvgCombined(){return ( this.averagecombined > -1 ) ? this.averagecombined : -1;}
    public int getMessagesDropped(){return ( this.dropped > -1 ) ? this.dropped : -1;}
    public int getRemainingCapacity(){return ( this.remainingcapacity > -1 ) ? this.remainingcapacity : -1;}

    public void setInFlight(int value){ this.inflight = value; }
    public void setAvgIn(int value){ this.averagein = value; }
    public void setAvgOut(int value){ this.averageout = value; }
    public void setAvgCombined(int value){ this.averagecombined = value; }
    public void setDroppedMessages(int value){ this.dropped = value; }
    public void setTimeStamp(long time) {this.timeStamp = time;}
    public void setSysexQueueCapacity(int sysexQueueCapacity) { this.remainingcapacity = sysexQueueCapacity; }
    public void setSysexQueueConsumed(int sysexQueueUsed) { this.usedcapacity = sysexQueueUsed; }
    public void setInflightTransactions(int value) {this.inflightTransactions = value;}
    public void setTimedOutTransactions(int value) {this.timedOutTransactions = value;}

}
