package MidiControl.unit.MidiDeviceManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import MidiControl.Telemetry.TelemetryData;

public class TelemetryTest {
    @Tag("Unit")
    @Test
    public void telemtryDtoSetTimeTest(){
        long confirmTime = java.time.Instant.EPOCH.getEpochSecond();
        TelemetryData dto = new TelemetryData();
        assertNotEquals(confirmTime, dto.getTimestamp());
        dto.setTimeStamp(java.time.Instant.EPOCH.getEpochSecond());
        assertEquals(confirmTime, dto.getTimestamp());
    }

    @Test
    public void telemtryDtoGetJson(){
        String confirmjson = "{\"type\":\"telemetry\",\"payload\":{\"timestamp\":0,\"inflight\":-1,\"dropped\":-1,"+
            "\"averagein\":-1,\"averageout\":-1,\"averagecombined\":-1,\"remainingcapacity\":-1,\"usedcapacity\":-1,\"inflightTransactions\":-1,\"timedOutTransactions\":-1} }";
        TelemetryData dtoJson = new TelemetryData();
        dtoJson.setTimeStamp(0);
        assertEquals(confirmjson, dtoJson.toJsonString());
        assertDoesNotThrow(() -> new Gson().toJson(dtoJson.toJsonString()));
    }

    @Test
    public void telemtryDtoGetAvgIn(){
        int confirmAvg = -1;
        TelemetryData dtoJson = new TelemetryData();
        assertEquals(confirmAvg, dtoJson.getAvgIn());
    }

    @Test
    public void telemtryDtoGetAvgOut(){
        int confirmAvg = -1;
        TelemetryData dtoJson = new TelemetryData();
        assertEquals(confirmAvg, dtoJson.getAvgOut());
    }

    @Test
    public void telemtryDtoGetAvgCombined(){
        int confirmAvg = -1;
        TelemetryData dtoJson = new TelemetryData();
        assertEquals(confirmAvg, dtoJson.getAvgCombined());
    }

    @Test
    public void setTelemetryData(){
        int confirmAvgIn=1;
        int confirmAvgOut=2;
        int confirmAvgCmb=3;
        int confirmInFlight=3;
        int confirmDropped=1;
        long confirmTime = java.time.Instant.now().getEpochSecond();
        TelemetryData dtoJson = new TelemetryData();
        dtoJson.setAvgCombined(confirmAvgCmb);
        dtoJson.setAvgIn(confirmAvgIn);
        dtoJson.setAvgOut(confirmAvgOut);
        dtoJson.setDroppedMessages(confirmDropped);
        dtoJson.setTimeStamp(confirmTime);
        dtoJson.setInFlightBytes(confirmInFlight);

        JsonObject json = (new Gson().fromJson(dtoJson.toJsonString(), JsonObject.class).get("payload").getAsJsonObject());
        assertEquals(confirmTime, json.get("timestamp").getAsLong());
        assertEquals(confirmDropped, json.get("dropped").getAsInt());
        assertEquals(confirmInFlight, json.get("inflight").getAsInt());
        assertEquals(confirmAvgIn, json.get("averagein").getAsInt());
        assertEquals(confirmAvgOut, json.get("averageout").getAsInt());
        assertEquals(confirmAvgCmb, json.get("averagecombined").getAsInt());
    }

    @Test
    public void getTelemetryData(){
        int confirmAvgIn=1;
        int confirmAvgOut=2;
        int confirmAvgCmb=3;
        int confirmInFlight=3;
        int confirmDropped=1;
        int confirmRemainingCapacity=20;
        long confirmTime = java.time.Instant.now().getEpochSecond();
        TelemetryData dtoJson = new TelemetryData();
        dtoJson.setAvgCombined(confirmAvgCmb);
        dtoJson.setAvgIn(confirmAvgIn);
        dtoJson.setAvgOut(confirmAvgOut);
        dtoJson.setDroppedMessages(confirmDropped);
        dtoJson.setTimeStamp(confirmTime);
        dtoJson.setInFlightBytes(confirmInFlight);
        dtoJson.setSysexQueueCapacity(confirmRemainingCapacity);

        assertEquals(confirmTime, dtoJson.getTimestamp());
        assertEquals(confirmDropped, dtoJson.getMessagesDropped());
        assertEquals(confirmInFlight, dtoJson.getInFlightBytes());
        assertEquals(confirmAvgIn, dtoJson.getAvgIn());
        assertEquals(confirmAvgOut, dtoJson.getAvgOut());
        assertEquals(confirmAvgCmb, dtoJson.getAvgCombined());
        assertEquals(confirmRemainingCapacity, dtoJson.getRemainingCapacity());
    }
}
